package com.example.readar

object Protocol {
    fun checksum(s: String): Int {
        var c = 0
        for (ch in s) c = c xor ch.code
        return c and 0xFF
    }

    fun frame(payload: String): String = "$payload*%02X\n".format(checksum(payload))

    fun unframe(line: String): String? {
        val i = line.lastIndexOf('*')
        if (i < 1 || line.length < i + 2) return null
        val payload = line.substring(0, i)
        val cs = line.substring(i + 1).trim().toIntOrNull(16) ?: return null
        return if (cs == checksum(payload)) payload else null
    }

    fun angleCmd(rawAngle: Int, buzzer: Boolean): String =
        frame("A,$rawAngle,${if (buzzer) 1 else 0}")

    fun rangeCmd(cm: Int): String = frame("R,$cm")

    fun parseMeasurement(payload: String): Measurement? {
        val p = payload.split(",")
        if (p.size != 4 || p[0] != "M") return null
        val a = p[1].toIntOrNull() ?: return null
        val d = p[2].toIntOrNull() ?: return null
        val s = p[3].toIntOrNull() ?: return null
        return Measurement(a, d, s == 1)
    }
}