package com.example.readar

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        setContent {
            RadarScreen(bluetoothAdapter)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun RadarScreen(bluetoothAdapter: BluetoothAdapter?) {
    var currentAngle by remember { mutableFloatStateOf(90f) }
    var currentDistance by remember { mutableIntStateOf(999) }
    var connectionStatus by remember { mutableStateOf("Disconnected") }

    var selectedRangeMeters by remember { mutableIntStateOf(3) }
    val maxRangeCm = selectedRangeMeters * 100f

    var radarMemory by remember { mutableStateOf(mapOf<Int, Float>()) }
    var btOutputStream by remember { mutableStateOf<OutputStream?>(null) }

    val scope = rememberCoroutineScope()
    val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val mirrorServoDirection = true

    fun changeRange(meters: Int) {
        selectedRangeMeters = meters
        radarMemory = emptyMap()
        scope.launch(Dispatchers.IO) {
            try {
                btOutputStream?.write(meters.toString().toByteArray())
                btOutputStream?.flush()
            } catch (_: IOException) { }
        }
    }

    fun updateRadarPersistence(rawAngle: Float, dist: Int) {
        val mappedAngle = if (mirrorServoDirection) (180f - rawAngle) else rawAngle
        currentAngle = mappedAngle
        currentDistance = dist

        val sweepSector = mappedAngle.roundToInt()
        val updatedMap = radarMemory.toMutableMap()

        for (a in (sweepSector - 2)..(sweepSector + 2)) {
            updatedMap.remove(a)
        }

        if (dist in 1..maxRangeCm.toInt()) {
            updatedMap[sweepSector] = dist.toFloat()
        }

        radarMemory = updatedMap
    }

    fun connectToHC06() {
        scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { connectionStatus = "Connecting to HC-06..." }
                val device = bluetoothAdapter?.bondedDevices?.find {
                    it.name.contains("HC-06", ignoreCase = true) || it.name.contains("HC-05", ignoreCase = true)
                }
                if (device == null) {
                    withContext(Dispatchers.Main) { connectionStatus = "Pair HC-06 in Phone Settings first!" }
                    return@launch
                }

                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()
                btOutputStream = socket.outputStream

                btOutputStream?.write(selectedRangeMeters.toString().toByteArray())
                btOutputStream?.flush()

                withContext(Dispatchers.Main) { connectionStatus = "Connected: ${device.name}" }

                val inputStream = socket.inputStream
                val buffer = StringBuilder()

                while (true) {
                    val byteRead = inputStream.read()
                    if (byteRead == -1) break
                    val char = byteRead.toChar()

                    if (char == '.') {
                        val parts = buffer.toString().split(",")
                        buffer.clear()
                        if (parts.size == 2) {
                            val angle = parts[0].trim().toFloatOrNull()
                            val dist = parts[1].trim().toIntOrNull()
                            if (angle != null && dist != null) {
                                withContext(Dispatchers.Main) {
                                    updateRadarPersistence(angle, dist)
                                }
                            }
                        }
                    } else {
                        buffer.append(char)
                    }
                }
            } catch (e: IOException) {
                btOutputStream = null
                withContext(Dispatchers.Main) { connectionStatus = "Connection Failed: ${e.message}" }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) connectToHC06()
        else connectionStatus = "Bluetooth permission denied!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A05))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ULTRASONIC RADAR (${selectedRangeMeters}M RANGE)",
            color = Color(0xFF00FF41),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = connectionStatus, color = Color.LightGray, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    } else {
                        connectToHC06()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
            ) {
                Text("Connect HC-06", color = Color.White)
            }

            OutlinedButton(onClick = { radarMemory = emptyMap() }) {
                Text("Clear Map", color = Color(0xFF00FF41))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RANGE:", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            listOf(1, 2, 3).forEach { meters ->
                val isSelected = selectedRangeMeters == meters
                Button(
                    onClick = { changeRange(meters) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF00FF41) else Color(0xFF1A241A)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${meters}M",
                        color = if (isSelected) Color.Black else Color(0xFF00FF41),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "ANGLE: ${currentAngle.toInt()}°",
                color = Color(0xFF00FF41),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            val distText = if (currentDistance <= maxRangeCm.toInt()) "${currentDistance} cm" else "CLEAR"
            val distColor = if (currentDistance <= maxRangeCm.toInt()) Color.Red else Color(0xFF00FF41)
            Text(
                text = "TARGET: $distText",
                color = distColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f)
            ) {
                val centerX = size.width / 2f
                val centerY = size.height - 20f
                val maxRadius = (size.width / 2f).coerceAtMost(size.height - 45f)

                val textPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#00FF41")
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                // වළලු සහ ඒ ඒ වළල්ලට අදාළ Distance Labels
                val ringLabels = when (selectedRangeMeters) {
                    1 -> listOf("50cm", "1m")
                    2 -> listOf("1m", "2m")
                    else -> listOf("1m", "2m", "3m")
                }

                val ringCount = ringLabels.size
                for (i in 1..ringCount) {
                    val r = maxRadius * (i / ringCount.toFloat())
                    drawArc(
                        color = Color(0xFF00FF41).copy(alpha = 0.45f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(centerX - r, centerY - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 2f)
                    )

                    // එක් එක් වළල්ලේ මුදුනට (90° line එකට) ඉහළින් දුර mark කිරීම
                    drawContext.canvas.nativeCanvas.drawText(
                        ringLabels[i - 1],
                        centerX,
                        centerY - r - 8f,
                        textPaint
                    )
                }

                val angles = listOf(30, 60, 90, 120, 150)
                for (deg in angles) {
                    val rad = Math.toRadians(deg.toDouble())
                    val endX = centerX + (maxRadius * cos(rad)).toFloat()
                    val endY = centerY - (maxRadius * sin(rad)).toFloat()
                    drawLine(
                        color = Color(0xFF00FF41).copy(alpha = 0.25f),
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f
                    )
                }

                val sweepRad = Math.toRadians(currentAngle.toDouble())
                val sweepEndX = centerX + (maxRadius * cos(sweepRad)).toFloat()
                val sweepEndY = centerY - (maxRadius * sin(sweepRad)).toFloat()
                drawLine(
                    color = Color(0xFF00FF41),
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY),
                    strokeWidth = 5f
                )

                radarMemory.forEach { (angleDeg, distCm) ->
                    val clampedBlipDist = distCm.coerceAtMost(maxRangeCm)
                    val blipRadius = (clampedBlipDist / maxRangeCm) * maxRadius
                    val blipRad = Math.toRadians(angleDeg.toDouble())
                    val bx = centerX + (blipRadius * cos(blipRad)).toFloat()
                    val by = centerY - (blipRadius * sin(blipRad)).toFloat()

                    val isCurrentlySweeping = abs(angleDeg - currentAngle) <= 3f
                    if (isCurrentlySweeping) {
                        drawCircle(
                            color = Color.Red.copy(alpha = 0.4f),
                            radius = 20f,
                            center = Offset(bx, by)
                        )
                    }
                    drawCircle(
                        color = Color.Red,
                        radius = 10f,
                        center = Offset(bx, by)
                    )
                }
            }
        }
    }
}