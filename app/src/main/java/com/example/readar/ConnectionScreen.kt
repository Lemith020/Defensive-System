package com.example.readar

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(onRequestPermissions: () -> Unit) {
    val state by RadarEngine.linkState.collectAsState()
    val message by RadarEngine.linkMessage.collectAsState()
    val devices by RadarEngine.devices.collectAsState()
    val stateColor = when (state) {
        LinkState.CONNECTED -> RadarGreen
        LinkState.CONNECTING -> Color(0xFFFFB300)
        LinkState.ERROR -> Color.Red
        LinkState.DISCONNECTED -> Color.Gray
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("BLUETOOTH", color = RadarGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Status: $state", color = stateColor, fontWeight = FontWeight.Bold)
        Text(message, color = Color.LightGray, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequestPermissions, modifier = Modifier.weight(1f)) { Text("Paired Devices") }
            Button(onClick = { RadarEngine.reconnect() }, modifier = Modifier.weight(1f)) { Text("Reconnect") }
        }
        Button(
            onClick = { RadarEngine.disconnect() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RadarRed)
        ) { Text("Disconnect", color = Color.White) }

        if (devices.isEmpty()) Text("No paired devices. Pair HC-06 in phone settings first.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices) { d ->
                val name = try { d.name } catch (_: SecurityException) { null } ?: "Unknown"
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { RadarEngine.connect(d) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A10))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(d.address, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}