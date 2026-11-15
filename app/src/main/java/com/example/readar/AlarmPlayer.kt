package com.example.readar

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

class AlarmPlayer(private val ctx: Context) {
    private var ringtone: Ringtone? = null

    fun start() {
        stop()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(ctx, uri) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            r.isLooping = true
        } else {
            @Suppress("DEPRECATION")
            r.streamType = AudioManager.STREAM_ALARM
        }
        r.play()
        ringtone = r
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}