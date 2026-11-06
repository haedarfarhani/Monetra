package ir.heydar.monetra.core.designsystem.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.theme.glassColors
import kotlin.math.roundToInt

/**
 * Smooth dual-series line chart with a gradient area fill, reveal animation, dashed average
 * lines and a touch tooltip. Dates run left-to-right regardless of locale direction.
 */
@Composable
fun MonetraLineChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    fullLabels: List<String> = labels,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    formatValue: (Float) -> String = { it.roundToInt().toString() },
    formatAxisValue: (Float) -> String = formatValue
) {
    val textMeasurer = rememberTextMeasurer()
    val glass = MaterialTheme.glassColors
    val colorScheme = MaterialTheme.colorScheme

    val progress = remember { Animatable(0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(labels, series) {
        selectedIndex = null
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
    val tooltipTitleStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = colorScheme.onSurface
    )
    val tooltipEntryStyle = MaterialTheme.typography.labelSmall.copy(color = colorScheme.onSurface)

    val gridColor = colorScheme.outlineVariant.copy(alpha = 0.35f)
    val guidelineColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Canvas(
        modifier = modifier.chartSelectionGestures(
            pointCount = { labels.size },
            onIndexSelected = { selectedIndex = it }
        )
    ) {
        if (labels.isEmpty() || series.isEmpty()) return@Canvas

        val pointCount = labels.size
        val labelPadBottom = 28f
        val padTop = 16f
        val padHorizontal = 8f
        val plotWidth = size.width - padHorizontal * 2
        val plotHeight = (size.height - labelPadBottom - padTop).coerceAtLeast(1f)
        val slotWidth = plotWidth / pointCount

        val rawMax = (series.flatMap { it.values } + referenceLines.map { it.value })
            .maxOrNull() ?: 1f
        val maxVal = niceMax(rawMax * 1.08f)

        // --- grid + axis values
        val midVal = maxVal / 2f
        val gridLines = listOf(
            maxVal to padTop,
            midVal to padTop + plotHeight / 2f,
            0f to padTop + plotHeight
        )
        gridLines.forEach { (value, y) ->
            drawGridLine(y, gridColor, padHorizontal, size.width - padHorizontal)
            if (value > 0f) {
                val layout = textMeasurer.measure(formatAxisValue(value), axisStyle)
                drawText(layout, topLeft = Offset(padHorizontal, y - layout.size.height - 2f))
            }
        }

        // --- series geometry (computed once, reused by fills, strokes and tooltips)
        val pointPositions = series.map { s ->
            List(pointCount) { index ->
                val raw = s.values.getOrNull(index) ?: 0f
                val clamped = raw.coerceIn(0f, maxVal)
                Offset(
                    x = padHorizontal + slotWidth * index + slotWidth / 2f,
                    y = padTop + plotHeight * (1f - clamped / maxVal)
                )
            }
        }

        val revealWidth = plotWidth * progress.value.coerceIn(0f, 1f)

        clipRect(
            left = padHorizontal,
            top = 0f,
            right = padHorizontal + revealWidth,
            bottom = size.height
        ) {
            pointPositions.forEachIndexed { seriesIndex, positions ->
                val color = series[seriesIndex].color
                val smooth = smoothPath(positions)

                // Gradient area fill under the curve.
                if (positions.size > 1) {
                    val fillPath = Path().apply {
                        addPath(smooth)
                        lineTo(positions.last().x, padTop + plotHeight)
                        lineTo(positions.first().x, padTop + plotHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startY = padTop,
                            endY = padTop + plotHeight
                        )
                    )
                }

                drawPath(
                    path = smooth,
                    color = color,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Small markers help when there are few points; dense charts stay clean.
            if (pointCount <= 20) {
                pointPositions.forEach { positions ->
                    positions.forEach { p ->
                        drawCircle(color = Color.White, radius = 3.dp.toPx() / 2f, center = p)
                    }
                }
                pointPositions.forEachIndexed { seriesIndex, positions ->
                    positions.forEach { p ->
                        drawCircle(
                            color = series[seriesIndex].color,
                            radius = 4.dp.toPx() / 2f,
                            center = p
                        )
                    }
                }
            }
        }

        // --- dashed reference (average) lines, above the reveal clip so they stay visible
        referenceLines.forEach { line ->
            val y = padTop + plotHeight * (1f - (line.value.coerceIn(0f, maxVal) / maxVal))
            val caption = textMeasurer.measure(
                "${line.title}: ${formatValue(line.value)}",
                axisStyle.copy(color = line.color.copy(alpha = 0.9f))
            )
            drawReferenceLine(
                y = y,
                startX = padHorizontal,
                endX = size.width - padHorizontal,
                color = line.color.copy(alpha = 0.75f),
                caption = caption
            )
        }

        // --- x labels (sampled to avoid crowding)
        val labelStep = if (pointCount <= 8) 1 else ((pointCount + 5) / 6)
        labels.forEachIndexed { index, label ->
            if (index % labelStep == 0 || index == pointCount - 1) {
                val layout = textMeasurer.measure(label, axisStyle)
                val centerX = padHorizontal + slotWidth * index + slotWidth / 2f
                drawTextCentered(
                    layout,
                    centerX = centerX.coerceIn(
                        padHorizontal + layout.size.width / 2f,
                        size.width - padHorizontal - layout.size.width / 2f
                    ),
                    y = size.height - 16f
                )
            }
        }

        // --- selection guideline, dots and tooltip
        val selected = selectedIndex?.takeIf { it in 0 until pointCount }
        if (selected != null) {
            val anchorX = padHorizontal + slotWidth * selected + slotWidth / 2f
            drawLine(
                color = guidelineColor,
                start = Offset(anchorX, padTop),
                end = Offset(anchorX, padTop + plotHeight),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            val tooltipEntries = mutableListOf<Pair<String, Color>>()
            pointPositions.forEachIndexed { seriesIndex, positions ->
                val p = positions[selected]
                drawCircle(
                    color = series[seriesIndex].color,
                    radius = 5.dp.toPx() / 2f,
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx() / 2f,
                    center = p
                )
                tooltipEntries +=
                    "${series[seriesIndex].title}: ${formatValue(series[seriesIndex].values[selected])}" to
                        series[seriesIndex].color
            }

            val anchorY = pointPositions.minOf { it[selected].y }
            drawChartTooltip(
                textMeasurer = textMeasurer,
                title = fullLabels.getOrNull(selected) ?: labels[selected],
                entries = tooltipEntries,
                titleStyle = tooltipTitleStyle,
                entryStyle = tooltipEntryStyle,
                anchorX = anchorX,
                anchorY = anchorY,
                surfaceColor = glass.dialogFillTop.copy(alpha = 0.96f),
                outlineColor = glass.strokeStart.copy(alpha = 0.4f),
                cornerRadius = 12.dp.toPx()
            )
        }
    }
}
