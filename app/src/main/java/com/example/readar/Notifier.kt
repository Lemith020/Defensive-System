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
    private val activeAlertIds = mutableListOf<Int>()

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val alertChannel = NotificationChannel(ALERT_CHANNEL, "Security Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            setSound(null, null)
            setShowBadge(true)
        }
        nm.createNotificationChannel(alertChannel)
        val serviceChannel = NotificationChannel(SERVICE_CHANNEL, "Radar Service", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        nm.createNotificationChannel(serviceChannel)
    }

    private fun openApp(ctx: Context, openAlerts: Boolean = false): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (openAlerts) putExtra("open_alerts", true)
        }
        val reqCode = if (openAlerts) 1 else 0
        return PendingIntent.getActivity(
            ctx,
            reqCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun serviceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, SERVICE_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Security Radar running")
            .setContentText("Scanning and monitoring")
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
            .build()

    @SuppressLint("MissingPermission")
    fun alert(ctx: Context, r: AlertRecord, unreadCount: Int = 1) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val text = "${alertMessage(r)} at ${formatTime(r.time)}"
        val n = NotificationCompat.Builder(ctx, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Security Alert: Object ${r.objectId} detected")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setNumber(unreadCount)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, openAlerts = true))
            .build()
        val currentId = alertId++
        synchronized(activeAlertIds) {
            activeAlertIds.add(currentId)
        }
        nm.notify(currentId, n)
    }

    @SuppressLint("MissingPermission")
    fun clearAlertNotifications(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        val idsToCancel = synchronized(activeAlertIds) {
            val list = activeAlertIds.toList()
            activeAlertIds.clear()
            list
        }
        idsToCancel.forEach { nm.cancel(it) }
        for (id in 100 until alertId) {
            nm.cancel(id)
        }
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