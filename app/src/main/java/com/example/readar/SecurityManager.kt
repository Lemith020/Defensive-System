package com.example.readar

class SecurityManager {
    private val hits = mutableMapOf<Int, Int>()
    private val alerted = mutableSetOf<Int>()

    fun evaluate(o: TrackedObject, securityOn: Boolean, securityCm: Int): Boolean {
        if (o.distance > securityCm + Config.ZONE_HYSTERESIS_CM) {
            hits.remove(o.id)
            alerted.remove(o.id)
            return false
        }
        if (o.distance > securityCm) return false
        val h = (hits[o.id] ?: 0) + 1
        hits[o.id] = h
        if (!securityOn || o.ignored) return false
        if (h < Config.ALERT_CONFIRM_HITS || o.id in alerted) return false
        alerted.add(o.id)
        return true
    }

    fun forget(id: Int) {
        hits.remove(id)
        alerted.remove(id)
    }
}