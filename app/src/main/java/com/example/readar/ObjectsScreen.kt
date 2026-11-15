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
fun ObjectsScreen() {
    val objects by RadarEngine.objects.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DETECTED OBJECTS (${objects.size})", color = RadarGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (objects.isEmpty()) Text("No objects tracked", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(objects, key = { it.id }) { o -> ObjectRow(o) }
        }
    }
}

@Composable
private fun ObjectRow(o: TrackedObject) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A10))) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    idLabel(o.id),
                    color = if (o.ignored) RadarIgnoredGreen else RadarRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("${o.angle.toInt()}°  •  ${o.distance.toInt()} cm", color = Color.White)
                Text(
                    "${o.status}${if (o.ignored) " • IGNORED" else ""}",
                    color = if (o.status == TrackStatus.LOST) Color(0xFFFFB300) else Color.LightGray,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { RadarEngine.setIgnored(o.id, !o.ignored) },
                colors = ButtonDefaults.buttonColors(containerColor = if (o.ignored) RadarRed else RadarIgnoredGreen)
            ) {
                Text(if (o.ignored) "UNIGNORE" else "IGNORE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}