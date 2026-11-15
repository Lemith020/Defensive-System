package com.example.readar

object Config {
    const val MIN_ANGLE = 15
    const val MAX_ANGLE = 165
    const val SCAN_STEP = 3
    const val MIRROR_SERVO = true
    const val MATCH_ANGLE_DEG = 8f
    const val MATCH_DIST_CM = 25f
    const val LOST_AFTER_MISSES = 1
    const val REMOVE_AFTER_MISSES = 4
    const val REMOVE_TIMEOUT_MS = 30_000L
    const val ALERT_CONFIRM_HITS = 2
    const val ZONE_HYSTERESIS_CM = 10
    const val RESPONSE_TIMEOUT_MS = 1500L
    const val MAX_RETRIES = 3
    const val ALARM_MS = 5000L
    val RANGES = listOf(100, 200, 300)
    val SECURITY_OPTIONS = listOf(50, 100, 150, 200, 250, 300)

    fun mirror(angle: Int): Int = if (MIRROR_SERVO) 180 - angle else angle
}