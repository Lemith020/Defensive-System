package com.example.readar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class TrackStatus { ACTIVE, LOST }

enum class LinkState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class TrackedObject(
    val id: Int,
    val angle: Float,
    val distance: Float,
    val x: Float,
    val y: Float,
    val firstDetected: Long,
    val lastDetected: Long,
    val lastScan: Int,
    val missedScans: Int,
    val status: TrackStatus,
    val ignored: Boolean
)

data class Measurement(val angle: Int, val distanceCm: Int, val valid: Boolean)

data class AlertRecord(val objectId: Int, val angle: Float, val distance: Float, val time: Long)

data class Settings(val rangeCm: Int = 300, val securityCm: Int = 100, val securityOn: Boolean = true)

object Polar {
    fun toXY(angleDeg: Float, dist: Float): Pair<Float, Float> {
        val rad = Math.toRadians(angleDeg.toDouble())
        return Pair((dist * cos(rad)).toFloat(), (dist * sin(rad)).toFloat())
    }
}

fun idLabel(id: Int): String = "#%03d".format(id)

fun formatTime(t: Long): String = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date(t))