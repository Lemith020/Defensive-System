package com.example.readar

import kotlin.math.abs

class ObjectTracker(initial: List<TrackedObject>, private var nextId: Int) {
    private val items = initial.toMutableList()
    private var scanNumber = 0

    init {
        nextId = maxOf(nextId, (items.maxOfOrNull { it.id } ?: 0) + 1)
    }

    fun snapshot(): List<TrackedObject> = items.sortedBy { it.id }

    fun nextIdValue(): Int = nextId

    fun update(angle: Float, dist: Float, now: Long): TrackedObject {
        val (x, y) = Polar.toXY(angle, dist)
        var bestIdx = -1
        var bestCost = Float.MAX_VALUE
        items.forEachIndexed { i, o ->
            val da = abs(o.angle - angle)
            val dd = abs(o.distance - dist)
            if (da <= Config.MATCH_ANGLE_DEG && dd <= Config.MATCH_DIST_CM) {
                val cost = da / Config.MATCH_ANGLE_DEG + dd / Config.MATCH_DIST_CM
                if (cost < bestCost) {
                    bestCost = cost
                    bestIdx = i
                }
            }
        }
        if (bestIdx >= 0) {
            val updated = items[bestIdx].copy(
                angle = angle,
                distance = dist,
                x = x,
                y = y,
                lastDetected = now,
                lastScan = scanNumber,
                missedScans = 0,
                status = TrackStatus.ACTIVE
            )
            items[bestIdx] = updated
            return updated
        }
        val created = TrackedObject(nextId++, angle, dist, x, y, now, now, scanNumber, 0, TrackStatus.ACTIVE, false)
        items.add(created)
        return created
    }

    fun completeScan(now: Long): List<Int> {
        val removed = mutableListOf<Int>()
        val kept = mutableListOf<TrackedObject>()
        for (o in items) {
            if (o.lastScan >= scanNumber) {
                kept.add(o)
                continue
            }
            val missed = o.missedScans + 1
            val expired = missed >= Config.REMOVE_AFTER_MISSES || now - o.lastDetected > Config.REMOVE_TIMEOUT_MS
            if (expired) {
                removed.add(o.id)
            } else {
                val status = if (missed >= Config.LOST_AFTER_MISSES) TrackStatus.LOST else o.status
                kept.add(o.copy(missedScans = missed, status = status))
            }
        }
        items.clear()
        items.addAll(kept)
        scanNumber++
        return removed
    }

    fun setIgnored(id: Int, ignored: Boolean) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(ignored = ignored)
    }

    fun removeBeyond(rangeCm: Int): List<Int> {
        val gone = items.filter { it.distance > rangeCm }.map { it.id }
        items.removeAll { it.distance > rangeCm }
        return gone
    }
}