package com.example.readar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen() {
    val objects by RadarEngine.objects.collectAsState()
    val angle by RadarEngine.sweepAngle.collectAsState()
    val dir by RadarEngine.sweepDir.collectAsState()
    val dist by RadarEngine.lastDistance.collectAsState()
    val settings by RadarEngine.settings.collectAsState()
    val state by RadarEngine.linkState.collectAsState()
    val message by RadarEngine.linkMessage.collectAsState()

    val stateColor = when (state) {
        LinkState.CONNECTED -> RadarGreen
        LinkState.CONNECTING -> Color(0xFFFFB300)
        LinkState.ERROR -> Color.Red
        LinkState.DISCONNECTED -> Color.Gray
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SMART SECURITY RADAR", color = RadarGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(message, color = stateColor, fontSize = 13.sp)
        Text(
            "Range ${settings.rangeCm / 100} m  •  Security ${if (settings.securityOn) "ON" else "OFF"} (${settings.securityCm} cm)",
            color = Color.LightGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        RadarCanvas(objects, angle, dir, settings.rangeCm, settings.securityCm)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("ANGLE: ${angle.toInt()}°", color = RadarGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                if (dist != null) "DIST: $dist cm" else "DIST: CLEAR",
                color = if (dist != null) Color.Red else RadarGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tracked ${objects.size}  •  Red ${objects.count { !it.ignored }}  •  Green ${objects.count { it.ignored }}",
            color = Color.LightGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Security Mode", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Switch(checked = settings.securityOn, onCheckedChange = { RadarEngine.setSecurityOn(it) })
        }
    }
}