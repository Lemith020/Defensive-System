package com.example.readar

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val ALERT_CHANNEL = "security_alerts"
    private const val SERVICE_CHANNEL = "radar_service"
    const val SERVICE_NOTIFICATION_ID = 1
    private var alertId = 100

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val alertChannel = NotificationChannel(ALERT_CHANNEL, "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
        alertChannel.enableVibration(true)
        alertChannel.setSound(null, null)
        nm.createNotificationChannel(alertChannel)
        nm.createNotificationChannel(
            NotificationChannel(SERVICE_CHANNEL, "Radar Service", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun serviceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, SERVICE_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Security Radar running")
            .setContentText("Scanning and monitoring")
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
            .build()

    @SuppressLint("MissingPermission")
    fun alert(ctx: Context, r: AlertRecord) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val text = "Object ${idLabel(r.objectId)} • ${r.distance.toInt()} cm • ${r.angle.toInt()}° • ${formatTime(r.time)}"
        val n = NotificationCompat.Builder(ctx, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Security Alert")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        nm.notify(alertId++, n)
    }

    @SuppressLint("MissingPermission")
    fun status(ctx: Context, title: String, text: String) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(ctx, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        nm.notify(99, n)
    }
}