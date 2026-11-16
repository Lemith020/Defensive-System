package com.example.readar

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun AppRoot() {
    var tab by remember { mutableIntStateOf(0) }
    val permissions = remember {
        buildList<String> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        RadarEngine.refreshDevices()
    }
    val requestPermissions: () -> Unit = {
        if (permissions.isEmpty()) RadarEngine.refreshDevices() else launcher.launch(permissions)
    }
    LaunchedEffect(Unit) { requestPermissions() }

    val titles = listOf("Radar", "Objects", "Settings", "Alerts", "Bluetooth")
    val icons = listOf(
        R.drawable.ic_nav_radar,
        R.drawable.ic_nav_objects,
        R.drawable.ic_nav_settings,
        R.drawable.ic_nav_alerts,
        R.drawable.ic_nav_bluetooth
    )
    val bg = Color(0xFF050A05)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RadarGreen,
            onPrimary = Color.Black,
            background = bg,
            surface = Color(0xFF0F1A10),
            onSurface = Color.White,
            onBackground = Color.White
        )
    ) {
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0A120A)) {
                    titles.forEachIndexed { i, title ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                Image(
                                    painter = painterResource(icons[i]),
                                    contentDescription = title,
                                    modifier = Modifier.size(32.dp).alpha(if (tab == i) 1f else 0.6f)
                                )
                            },
                            label = { Text(title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = RadarGreen.copy(alpha = 0.18f),
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).statusBarsPadding()) {
                when (tab) {
                    0 -> DashboardScreen()
                    1 -> ObjectsScreen()
                    2 -> SettingsScreen()
                    3 -> AlertsScreen()
                    else -> ConnectionScreen(requestPermissions)
                }
            }
        }
    }
}