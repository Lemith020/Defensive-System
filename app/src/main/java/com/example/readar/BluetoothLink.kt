package com.example.readar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothLink(private val adapter: BluetoothAdapter?) {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var output: OutputStream? = null

    fun pairedDevices(): List<BluetoothDevice> = adapter?.bondedDevices?.toList() ?: emptyList()

    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        close()
        val s = device.createRfcommSocketToServiceRecord(uuid)
        try {
            s.connect()
        } catch (e: IOException) {
            try { s.close() } catch (_: IOException) { }
            throw e
        }
        socket = s
        output = s.outputStream
    }

    suspend fun send(text: String): Boolean = withContext(Dispatchers.IO) {
        val o = output ?: return@withContext false
        try {
            o.write(text.toByteArray())
            o.flush()
            true
        } catch (_: IOException) {
            false
        }
    }

    suspend fun readLoop(onLine: (String) -> Unit) = withContext(Dispatchers.IO) {
        val input = socket?.inputStream ?: return@withContext
        val sb = StringBuilder()
        try {
            while (true) {
                val b = input.read()
                if (b == -1) break
                val c = b.toChar()
                if (c == '\n') {
                    val line = sb.toString().trim()
                    sb.clear()
                    if (line.isNotEmpty()) onLine(line)
                } else if (sb.length < 64) {
                    sb.append(c)
                } else {
                    sb.clear()
                }
            }
        } catch (_: IOException) { }
    }

    fun close() {
        try { socket?.close() } catch (_: IOException) { }
        socket = null
        output = null
    }
}