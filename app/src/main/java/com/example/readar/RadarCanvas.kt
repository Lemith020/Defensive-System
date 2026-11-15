package com.example.readar

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin



@Composable
fun RadarCanvas(
    objects: List<TrackedObject>,
    sweepAngle: Float,
    sweepDir: Int,
    rangeCm: Int,
    securityCm: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1.8f)) {
        val margin = size.width * 0.09f
        val cx = size.width / 2f
        val cy = size.height - margin * 0.9f
        val radius = minOf(size.width / 2f - margin, cy - margin)
        val topLeft = Offset(cx - radius, cy - radius)
        val full = Size(radius * 2, radius * 2)
        val canvas = drawContext.canvas.nativeCanvas

        val axisPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00FF41")
            textSize = size.width * 0.032f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val tagPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = size.width * 0.03f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        fun pt(deg: Float, r: Float): Offset {
            val rad = Math.toRadians(deg.toDouble())
            return Offset(cx + r * cos(rad).toFloat(), cy - r * sin(rad).toFloat())
        }

        drawArc(
            brush = Brush.radialGradient(
                listOf(Color(0xFF020A03), Color(0xFF0E4A1A)),
                center = Offset(cx, cy),
                radius = radius
            ),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = topLeft,
            size = full
        )

        val ringLabels = when (rangeCm) {
            100 -> listOf("50cm", "1m")
            200 -> listOf("1m", "2m")
            else -> listOf("1m", "2m", "3m")
        }
        ringLabels.forEachIndexed { i, label ->
            val r = radius * (i + 1) / ringLabels.size
            drawArc(
                color = RadarGreen.copy(alpha = 0.5f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2f)
            )
            canvas.drawText(label, cx + r - axisPaint.textSize, cy + axisPaint.textSize * 1.1f, axisPaint)
        }

        for (deg in 0..180 step 15) {
            drawLine(
                color = RadarGreen.copy(alpha = if (deg % 30 == 0) 0.3f else 0.12f),
                start = Offset(cx, cy),
                end = pt(deg.toFloat(), radius),
                strokeWidth = 1.5f
            )
        }

        if (securityCm in 1..rangeCm) {
            val r = radius * securityCm / rangeCm
            drawArc(
                color = Color(0xFFFFB300).copy(alpha = 0.85f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            )
        }

        drawArc(
            color = RadarGreen,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = full,
            style = Stroke(width = 4f)
        )
        drawLine(RadarGreen, Offset(cx - radius, cy), Offset(cx + radius, cy), 3f)

        for (deg in 0..180 step 5) {
            val major = deg % 15 == 0
            val len = if (major) radius * 0.045f else radius * 0.022f
            drawLine(
                color = RadarGreen,
                start = pt(deg.toFloat(), radius - len),
                end = pt(deg.toFloat(), radius),
                strokeWidth = if (major) 3f else 1.5f
            )
        }
        for (deg in 0..180 step 30) {
            val p = pt(deg.toFloat(), radius + margin * 0.55f)
            canvas.drawText("$deg", p.x, p.y + axisPaint.textSize / 3f, axisPaint)
        }

        val trailSteps = 16
        for (i in 0 until trailSteps) {
            val e1 = sweepAngle - sweepDir * i * 1.6f
            val e2 = sweepAngle - sweepDir * (i + 1) * 1.6f
            val lo = minOf(e1, e2).coerceIn(0f, 180f)
            val hi = maxOf(e1, e2).coerceIn(0f, 180f)
            if (hi - lo <= 0f) continue
            drawArc(
                color = RadarGreen.copy(alpha = 0.32f * (1f - i / trailSteps.toFloat())),
                startAngle = -hi,
                sweepAngle = hi - lo,
                useCenter = true,
                topLeft = topLeft,
                size = full
            )
        }
        drawLine(RadarGreen, Offset(cx, cy), pt(sweepAngle, radius), 5f)

        objects.forEach { o ->
            val sx = cx + o.x / rangeCm * radius
            val sy = cy - o.y / rangeCm * radius
            val base = if (o.ignored) RadarIgnoredGreen else RadarRed
            val alpha = if (o.status == TrackStatus.LOST) 0.45f else 1f
            drawCircle(base.copy(alpha = 0.25f * alpha), 22f, Offset(sx, sy))
            drawCircle(base.copy(alpha = alpha), 10f, Offset(sx, sy))
            val half = 24f
            val len = 9f
            val bracket = Color.White.copy(alpha = alpha)
            for (dx in listOf(-1f, 1f)) {
                for (dy in listOf(-1f, 1f)) {
                    val px = sx + dx * half
                    val py = sy + dy * half
                    drawLine(bracket, Offset(px, py), Offset(px - dx * len, py), 2f)
                    drawLine(bracket, Offset(px, py), Offset(px, py - dy * len), 2f)
                }
            }
            canvas.drawText(idLabel(o.id), sx, sy - half - 6f, tagPaint)
        }
    }
}