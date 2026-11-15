package com.example.readar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun SettingsScreen() {
    val s by RadarEngine.settings.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("SECURITY SETTINGS", color = RadarGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ChoiceRow("Detection Range", Config.RANGES, s.rangeCm, { "${it / 100} m" }) { RadarEngine.setRange(it) }
        ChoiceRow(
            "Security Distance (cm)",
            Config.SECURITY_OPTIONS.filter { it <= s.rangeCm },
            s.securityCm,
            { "$it" }
        ) { RadarEngine.setSecurityDistance(it) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Security Mode", color = Color.White, fontWeight = FontWeight.Bold)
            Switch(checked = s.securityOn, onCheckedChange = { RadarEngine.setSecurityOn(it) })
        }
    }
}

@Composable
fun ChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.LightGray, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { v ->
                val sel = v == selected
                Button(
                    onClick = { onSelect(v) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (sel) RadarGreen else Color(0xFF1A241A))
                ) {
                    Text(label(v), color = if (sel) Color.Black else RadarGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}