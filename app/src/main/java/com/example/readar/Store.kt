package com.example.readar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(context: Context) {
    private val p = context.getSharedPreferences("radar_store", Context.MODE_PRIVATE)

    var lastDevice: String?
        get() = p.getString("lastDevice", null)
        set(v) { p.edit().putString("lastDevice", v).apply() }

    fun loadSettings() = Settings(
        p.getInt("range", 300),
        p.getInt("security", 100),
        p.getBoolean("securityOn", true)
    )

    fun saveSettings(s: Settings) {
        p.edit()
            .putInt("range", s.rangeCm)
            .putInt("security", s.securityCm)
            .putBoolean("securityOn", s.securityOn)
            .apply()
    }

    fun saveObjects(list: List<TrackedObject>, nextId: Int) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("a", it.angle.toDouble())
                    .put("d", it.distance.toDouble())
                    .put("f", it.firstDetected)
                    .put("ig", it.ignored)
            )
        }
        p.edit().putString("objects", arr.toString()).putInt("nextId", nextId).apply()
    }

    fun loadNextId(): Int = p.getInt("nextId", 1)

    fun loadObjects(): List<TrackedObject> {
        val arr = try { JSONArray(p.getString("objects", "[]")) } catch (_: Exception) { JSONArray() }
        val now = System.currentTimeMillis()
        val out = mutableListOf<TrackedObject>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.optBoolean("ig")) continue
            val a = o.getDouble("a").toFloat()
            val d = o.getDouble("d").toFloat()
            val (x, y) = Polar.toXY(a, d)
            out.add(TrackedObject(o.getInt("id"), a, d, x, y, o.getLong("f"), now, -1, 0, TrackStatus.ACTIVE, true))
        }
        return out
    }

    fun saveAlerts(list: List<AlertRecord>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.objectId)
                    .put("a", it.angle.toDouble())
                    .put("d", it.distance.toDouble())
                    .put("t", it.time)
            )
        }
        p.edit().putString("alerts", arr.toString()).apply()
    }

    fun loadAlerts(): List<AlertRecord> {
        val arr = try { JSONArray(p.getString("alerts", "[]")) } catch (_: Exception) { JSONArray() }
        val out = mutableListOf<AlertRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(AlertRecord(o.getInt("id"), o.getDouble("a").toFloat(), o.getDouble("d").toFloat(), o.getLong("t")))
        }
        return out
    }
}

