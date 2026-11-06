package ir.heydar.monetra.core.designsystem.charts

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Shared plumbing for the Monetra hand-drawn chart kit: gesture-to-index mapping, smooth
 * Catmull-Rom paths, axis helpers and the shared touch tooltip.
 */

/** One data series of a line or bar chart. Values are already in display units. */
data class ChartSeries(
    val title: String,
    val values: List<Float>,
    val color: Color
)

/** Dashed horizontal reference line (e.g. an average). */
data class ChartReferenceLine(
    val title: String,
    val value: Float,
    val color: Color
)

/** Maps an x position inside the plot area to the nearest data index. */
internal fun nearestIndex(
    x: Float,
    width: Int,
    pointCount: Int,
    leftPad: Float = 0f,
    rightPad: Float = 0f
): Int {
    if (pointCount <= 0) return 0
    val plotWidth = (width - leftPad - rightPad).coerceAtLeast(1f)
    val relative = ((x - leftPad) / plotWidth * pointCount).toInt()
    return relative.coerceIn(0, pointCount - 1)
}

/** Tap + horizontal drag selection that reports the nearest chart index. */
internal fun Modifier.chartSelectionGestures(
    pointCount: () -> Int,
    onIndexSelected: (Int?) -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures { offset ->
            val count = pointCount()
            onIndexSelected(if (count == 0) null else nearestIndex(offset.x, size.width, count))
        }
    }
    .pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                val count = pointCount()
                onIndexSelected(if (count == 0) null else nearestIndex(offset.x, size.width, count))
            },
            onHorizontalDrag = { change, _ ->
                val count = pointCount()
                if (count > 0) {
                    change.consume()
                    onIndexSelected(nearestIndex(change.position.x, size.width, count))
                }
            }
        )
    }

/** Smooth Catmull-Rom curve through the given points. */
internal fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 0 until points.size - 1) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    return path
}

/** Rounds a raw max up to a friendlier axis bound so the top gridline has a tidy value. */
internal fun niceMax(raw: Float): Float {
    if (raw <= 0f) return 1f
    val exponent = 10f.pow(floor(log10(raw.toDouble())).toFloat())
    val normalized = raw / exponent
    val nice = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 2.5f -> 2.5f
        normalized <= 5f -> 5f
        else -> 10f
    }
    return nice * exponent
}

/** Hairline horizontal grid line. */
internal fun DrawScope.drawGridLine(
    y: Float,
    color: Color,
    startX: Float,
    endX: Float
) {
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 1f
    )
}

/** Dashed horizontal reference line with an optional trailing caption. */
internal fun DrawScope.drawReferenceLine(
    y: Float,
    startX: Float,
    endX: Float,
    color: Color,
    caption: TextLayoutResult?
) {
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    )
    if (caption != null) {
        drawText(caption, topLeft = Offset(endX - caption.size.width, y - caption.size.height - 4f))
    }
}

/**
 * Rounded glassy tooltip anchored above the selected point: title line + one colored-dot entry
 * per series. Used by both the line and bar charts.
 */
internal fun DrawScope.drawChartTooltip(
    textMeasurer: TextMeasurer,
    title: String,
    entries: List<Pair<String, Color>>,
    titleStyle: TextStyle,
    entryStyle: TextStyle,
    anchorX: Float,
    anchorY: Float,
    surfaceColor: Color,
    outlineColor: Color,
    cornerRadius: Float
) {
    val titleLayout = textMeasurer.measure(title, titleStyle)
    val entryLayouts = entries.map { (text, _) -> textMeasurer.measure(text, entryStyle) }

    val maxTextWidth = (listOf(titleLayout) + entryLayouts).maxOf { it.size.width }
    val dotSpace = entryStyle.fontSize.toPx() + 10f
    val tooltipWidth = (maxTextWidth + dotSpace + 20f).coerceAtLeast(96f)
    val tooltipHeight = titleLayout.size.height + entryLayouts.sumOf { it.size.height } +
        10f + (entryLayouts.size * 4f)

    val left = (anchorX - tooltipWidth / 2f)
        .coerceIn(4f, (size.width - tooltipWidth - 4f).coerceAtLeast(4f))
    val top = (anchorY - tooltipHeight - 16f).coerceAtLeast(4f)

    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(left, top),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(left, top),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 1f)
    )

    var y = top + 10f
    drawText(titleLayout, topLeft = Offset(left + 12f, y))
    y += titleLayout.size.height + 6f

    entries.forEachIndexed { index, (text, color) ->
        val layout = entryLayouts[index]
        val centerY = y + layout.size.height / 2f
        drawCircle(
            color = color,
            radius = entryStyle.fontSize.toPx() * 0.35f,
            center = Offset(left + 12f + entryStyle.fontSize.toPx() * 0.3f, centerY)
        )
        drawText(layout, topLeft = Offset(left + 12f + dotSpace, y))
        y += layout.size.height + 4f
    }
}

/** Draws a pre-measured label centered horizontally on [centerX]. */
internal fun DrawScope.drawTextCentered(
    layout: TextLayoutResult,
    centerX: Float,
    y: Float
) {
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, y))
}
