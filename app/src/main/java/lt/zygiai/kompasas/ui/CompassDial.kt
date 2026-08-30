package lt.zygiai.kompasas.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import lt.zygiai.kompasas.ui.theme.CompassPalette
import lt.zygiai.kompasas.ui.theme.LocalCompassPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassDial(
    continuousHeading: Float,
    targetCourse: Float?,
    modifier: Modifier = Modifier
) {
    val palette = LocalCompassPalette.current
    val animatedHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 240f),
        label = "compassRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = min(size.width, size.height) * 0.46f
            val center = this.center

            drawCircle(color = palette.dialFace, radius = radius, center = center)
            if (palette.isTopographic || palette.topographicLines.alpha > 0f) {
                drawTopographicPattern(center, radius, palette)
            }
            drawCircle(
                color = palette.dialRing,
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = palette.tickMinor.copy(alpha = 0.45f),
                radius = radius * 0.84f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            rotate(degrees = -animatedHeading, pivot = center) {
                drawTicks(center, radius, palette)
                drawDegreeLabels(center, radius, palette)
                drawCardinals(center, radius, palette)
                drawNorthSouthNeedle(center, radius, palette)
                targetCourse?.let { drawTargetMarker(center, radius, it, palette) }
            }

            drawFixedPointer(center, radius, palette)
            drawCircle(color = palette.dialRing, radius = radius * 0.045f, center = center)
            drawCircle(color = palette.dialFace, radius = radius * 0.022f, center = center)
        }
    }
}

private fun DrawScope.drawTicks(center: Offset, radius: Float, palette: CompassPalette) {
    for (degree in 0 until 360 step 5) {
        val isThirty = degree % 30 == 0
        val isTen = degree % 10 == 0
        val outer = radius * 0.96f
        val inner = when {
            isThirty -> radius * 0.82f
            isTen -> radius * 0.86f
            else -> radius * 0.90f
        }
        val angle = Math.toRadians((degree - 90).toDouble())
        val start = Offset(
            center.x + cos(angle).toFloat() * inner,
            center.y + sin(angle).toFloat() * inner
        )
        val end = Offset(
            center.x + cos(angle).toFloat() * outer,
            center.y + sin(angle).toFloat() * outer
        )
        drawLine(
            color = if (isTen) palette.tickMajor else palette.tickMinor,
            start = start,
            end = end,
            strokeWidth = when {
                isThirty -> 3.dp.toPx()
                isTen -> 2.dp.toPx()
                else -> 1.dp.toPx()
            }
        )
    }
}

private fun DrawScope.drawDegreeLabels(center: Offset, radius: Float, palette: CompassPalette) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.textSecondary.toArgbCompat()
        textAlign = Paint.Align.CENTER
        textSize = radius * 0.065f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    for (degree in 0 until 360 step 30) {
        if (degree % 90 == 0) continue
        val angle = Math.toRadians((degree - 90).toDouble())
        val textRadius = radius * 0.72f
        val x = center.x + cos(angle).toFloat() * textRadius
        val y = center.y + sin(angle).toFloat() * textRadius - (paint.ascent() + paint.descent()) / 2f
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(degree.toString(), x, y, paint) }
    }
}

private fun DrawScope.drawCardinals(center: Offset, radius: Float, palette: CompassPalette) {
    val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = radius * 0.14f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.textSecondary.toArgbCompat()
        textAlign = Paint.Align.CENTER
        textSize = radius * 0.062f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val labels = listOf(
        Triple(0f, "N", palette.north),
        Triple(90f, "E", palette.textPrimary),
        Triple(180f, "S", palette.south),
        Triple(270f, "W", palette.textPrimary)
    )
    labels.forEach { (degree, label, color) ->
        cardinalPaint.color = color.toArgbCompat()
        val angle = Math.toRadians((degree - 90).toDouble())
        val textRadius = radius * 0.70f
        val x = center.x + cos(angle).toFloat() * textRadius
        val y = center.y + sin(angle).toFloat() * textRadius -
            (cardinalPaint.ascent() + cardinalPaint.descent()) / 2f
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(label, x, y, cardinalPaint) }
    }

    listOf(45f to "NE", 135f to "SE", 225f to "SW", 315f to "NW").forEach { (degree, label) ->
        val angle = Math.toRadians((degree - 90).toDouble())
        val textRadius = radius * 0.70f
        val x = center.x + cos(angle).toFloat() * textRadius
        val y = center.y + sin(angle).toFloat() * textRadius -
            (secondaryPaint.ascent() + secondaryPaint.descent()) / 2f
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(label, x, y, secondaryPaint) }
    }
}

private fun DrawScope.drawNorthSouthNeedle(center: Offset, radius: Float, palette: CompassPalette) {
    val northPath = Path().apply {
        moveTo(center.x, center.y - radius * 0.58f)
        lineTo(center.x - radius * 0.075f, center.y)
        lineTo(center.x + radius * 0.075f, center.y)
        close()
    }
    val southPath = Path().apply {
        moveTo(center.x, center.y + radius * 0.58f)
        lineTo(center.x - radius * 0.075f, center.y)
        lineTo(center.x + radius * 0.075f, center.y)
        close()
    }
    drawPath(northPath, palette.north.copy(alpha = 0.86f))
    drawPath(southPath, palette.south.copy(alpha = 0.76f))
}

private fun DrawScope.drawTargetMarker(
    center: Offset,
    radius: Float,
    targetCourse: Float,
    palette: CompassPalette
) {
    val angle = Math.toRadians((targetCourse - 90f).toDouble())
    val inner = radius * 0.77f
    val outer = radius * 0.985f
    val start = Offset(
        center.x + cos(angle).toFloat() * inner,
        center.y + sin(angle).toFloat() * inner
    )
    val end = Offset(
        center.x + cos(angle).toFloat() * outer,
        center.y + sin(angle).toFloat() * outer
    )
    drawLine(
        color = palette.target,
        start = start,
        end = end,
        strokeWidth = 5.dp.toPx()
    )
    drawCircle(color = palette.target, radius = 5.dp.toPx(), center = end)
}

private fun DrawScope.drawFixedPointer(center: Offset, radius: Float, palette: CompassPalette) {
    val y = center.y - radius - 4.dp.toPx()
    val path = Path().apply {
        moveTo(center.x, y + 17.dp.toPx())
        lineTo(center.x - 10.dp.toPx(), y)
        lineTo(center.x + 10.dp.toPx(), y)
        close()
    }
    drawPath(path, palette.textPrimary)
}

private fun DrawScope.drawTopographicPattern(
    center: Offset,
    radius: Float,
    palette: CompassPalette
) {
    for (i in 1..6) {
        val shiftX = ((i % 2) * 2 - 1) * radius * 0.05f
        val shiftY = ((i % 3) - 1) * radius * 0.045f
        drawCircle(
            color = palette.topographicLines.copy(alpha = if (palette.isTopographic) 0.65f else 0.22f),
            radius = radius * (0.18f + i * 0.095f),
            center = Offset(center.x + shiftX, center.y + shiftY),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun androidx.compose.ui.graphics.Color.toArgbCompat(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
