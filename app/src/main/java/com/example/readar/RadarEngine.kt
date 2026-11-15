package com.example.readar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
object RadarEngine {
    private lateinit var ctx: Context
    private lateinit var store: Store
    private lateinit var link: BluetoothLink
    private lateinit var alarm: AlarmPlayer
    private lateinit var tracker: ObjectTracker
    private val security = SecurityManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private val responses = Channel<String>(Channel.UNLIMITED)
    private var scanJob: Job? = null
    private var alarmJob: Job? = null
    @Volatile private var buzzer = false

    val linkState = MutableStateFlow(LinkState.DISCONNECTED)
    val linkMessage = MutableStateFlow("Disconnected")
    val sweepAngle = MutableStateFlow(90f)
    val sweepDir = MutableStateFlow(1)
    val lastDistance = MutableStateFlow<Int?>(null)
    val objects = MutableStateFlow<List<TrackedObject>>(emptyList())
    val alerts = MutableStateFlow<List<AlertRecord>>(emptyList())
    val settings = MutableStateFlow(Settings())
    val devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    fun init(context: Context) {
        ctx = context.applicationContext
        store = Store(ctx)
        link = BluetoothLink(ctx.getSystemService(BluetoothManager::class.java)?.adapter)
        alarm = AlarmPlayer(ctx)
        settings.value = store.loadSettings()
        tracker = ObjectTracker(store.loadObjects(), store.loadNextId())
        alerts.value = store.loadAlerts()
        objects.value = tracker.snapshot()
        Notifier.createChannels(ctx)
    }

    fun refreshDevices() {
        devices.value = try { link.pairedDevices() } catch (_: SecurityException) { emptyList() }
    }

    fun reconnect() {
        val address = store.lastDevice ?: return
        val device = try { link.pairedDevices().find { it.address == address } } catch (_: SecurityException) { null }
        if (device != null) connect(device)
    }

    fun connect(device: BluetoothDevice) {
        if (linkState.value == LinkState.CONNECTING || linkState.value == LinkState.CONNECTED) return
        linkState.value = LinkState.CONNECTING
        linkMessage.value = "Connecting to ${device.name ?: device.address}..."
        scanJob = scope.launch {
            try {
                link.connect(device)
            } catch (e: Exception) {
                linkState.value = LinkState.ERROR
                linkMessage.value = "Connection failed: ${e.message}"
                return@launch
            }
            store.lastDevice = device.address
            linkState.value = LinkState.CONNECTED
            linkMessage.value = "Connected: ${device.name ?: device.address}"
            RadarService.start(ctx)
            launch {
                link.readLoop { line -> Protocol.unframe(line)?.let { responses.trySend(it) } }
                if (linkState.value == LinkState.CONNECTED) handleLost("Connection lost")
            }
            runScan()
        }
    }

    fun disconnect() {
        linkState.value = LinkState.DISCONNECTED
        linkMessage.value = "Disconnected"
        link.close()
        scanJob?.cancel()
        stopAlarm()
        RadarService.stop(ctx)
    }

    private fun handleLost(message: String) {
        linkState.value = LinkState.ERROR
        linkMessage.value = message
        link.close()
        stopAlarm()
        RadarService.stop(ctx)
        Notifier.status(ctx, "Radar disconnected", message)
        scanJob?.cancel()
    }

    private suspend fun exchange(cmd: String, accept: (String) -> Boolean): String? {
        repeat(Config.MAX_RETRIES) {
            while (responses.tryReceive().isSuccess) { }
            if (!link.send(cmd)) return null
            val r = withTimeoutOrNull(Config.RESPONSE_TIMEOUT_MS) {
                var p = responses.receive()
                while (!accept(p)) p = responses.receive()
                p
            }
            if (r != null) return r
        }
        return null
    }

    private suspend fun runScan() {
        var angle = Config.MIN_ANGLE
        var dir = 1
        var appliedRange = -1
        sweepDir.value = 1
        while (true) {
            val wantRange = settings.value.rangeCm
            if (wantRange != appliedRange) {
                val ack = exchange(Protocol.rangeCmd(wantRange)) { it == "K,R,$wantRange" }
                if (ack == null) {
                    handleLost("Arduino not responding")
                    return
                }
                appliedRange = wantRange
            }
            val raw = Config.mirror(angle)
            val resp = exchange(Protocol.angleCmd(raw, buzzer)) { Protocol.parseMeasurement(it)?.angle == raw }
            if (resp == null) {
                handleLost("Arduino not responding")
                return
            }
            val m = Protocol.parseMeasurement(resp) ?: continue
            processMeasurement(m)
            if ((dir > 0 && angle >= Config.MAX_ANGLE) || (dir < 0 && angle <= Config.MIN_ANGLE)) {
                finishPass()
                dir = -dir
                sweepDir.value = dir
            }
            angle = (angle + dir * Config.SCAN_STEP).coerceIn(Config.MIN_ANGLE, Config.MAX_ANGLE)
        }
    }

    private fun processMeasurement(m: Measurement) {
        val display = Config.mirror(m.angle).toFloat()
        sweepAngle.value = display
        val s = settings.value
        val valid = m.valid && m.distanceCm in 1..s.rangeCm
        lastDistance.value = if (valid) m.distanceCm else null
        if (!valid) return
        val now = System.currentTimeMillis()
        val (obj, fire) = synchronized(lock) {
            val o = tracker.update(display, m.distanceCm.toFloat(), now)
            o to security.evaluate(o, s.securityOn, s.securityCm)
        }
        publish()
        if (fire) raiseAlert(obj, now)
    }

    private fun finishPass() {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            tracker.completeScan(now).forEach { security.forget(it) }
        }
        publish(true)
    }

    private fun publish(persist: Boolean = false) {
        val (snap, nextId) = synchronized(lock) { tracker.snapshot() to tracker.nextIdValue() }
        objects.value = snap
        if (persist) store.saveObjects(snap, nextId)
    }

    private fun raiseAlert(o: TrackedObject, now: Long) {
        val rec = AlertRecord(o.id, o.angle, o.distance, now)
        alerts.update { (listOf(rec) + it).take(100) }
        store.saveAlerts(alerts.value)
        Notifier.alert(ctx, rec)
        val previous = alarmJob
        alarmJob = scope.launch(Dispatchers.Main) {
            previous?.cancelAndJoin()
            try {
                buzzer = true
                alarm.start()
                delay(Config.ALARM_MS)
            } finally {
                alarm.stop()
                buzzer = false
            }
        }
    }

    fun stopAlarm() {
        alarmJob?.cancel()
    }

    fun setIgnored(id: Int, ignored: Boolean) {
        synchronized(lock) { tracker.setIgnored(id, ignored) }
        publish(true)
    }

    fun setRange(cm: Int) {
        settings.update { it.copy(rangeCm = cm, securityCm = minOf(it.securityCm, cm)) }
        store.saveSettings(settings.value)
        synchronized(lock) { tracker.removeBeyond(cm).forEach { security.forget(it) } }
        publish(true)
    }

    fun setSecurityDistance(cm: Int) {
        settings.update { it.copy(securityCm = cm) }
        store.saveSettings(settings.value)
    }

    fun setSecurityOn(on: Boolean) {
        settings.update { it.copy(securityOn = on) }
        store.saveSettings(settings.value)
        if (!on) stopAlarm()
    }

    fun clearAlerts() {
        alerts.value = emptyList()
        store.saveAlerts(emptyList())
    }
}