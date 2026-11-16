package com.example.readar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun AlertsScreen() {
    val alerts by RadarEngine.alerts.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SECURITY ALERTS (${alerts.size})", color = RadarGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { RadarEngine.stopAlarm() },
                colors = ButtonDefaults.buttonColors(containerColor = RadarRed)
            ) { Text("Stop Alarm", color = Color.White) }
            OutlinedButton(onClick = { RadarEngine.clearAlerts() }) { Text("Clear History", color = RadarGreen) }
        }
        if (alerts.isEmpty()) Text("No alerts yet", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(alerts) { a ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F0F))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Security Alert: Object ${a.objectId} detected", color = RadarRed, fontWeight = FontWeight.Bold)
                        Text("Someone or something entered the security zone.", color = Color.White, fontSize = 14.sp)
                        Text("Distance: ${a.distance.toInt()} cm", color = Color.White)
                        Text("Direction: ${directionLabel(a.angle).replaceFirstChar { it.uppercase() }} (${a.angle.toInt()} degrees)", color = Color.White)
                        Text("Time: ${formatTime(a.time)}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}