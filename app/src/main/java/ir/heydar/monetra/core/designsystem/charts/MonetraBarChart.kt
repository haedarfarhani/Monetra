package ir.heydar.monetra.core.designsystem.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.theme.glassColors
import kotlin.math.roundToInt

/**
 * Grouped bar chart with vertical gradient bars, grow animation and a touch tooltip.
 * Used for the monthly (income vs expense) report view.
 */
@Composable
fun MonetraBarChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    fullLabels: List<String> = labels,
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
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
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

        val rawMax = series.flatMap { it.values }.maxOrNull() ?: 1f
        val maxVal = niceMax(rawMax * 1.08f)

        // --- grid + axis values
        val midVal = maxVal / 2f
        listOf(
            maxVal to padTop,
            midVal to padTop + plotHeight / 2f,
            0f to padTop + plotHeight
        ).forEach { (value, y) ->
            drawGridLine(y, gridColor, padHorizontal, size.width - padHorizontal)
            if (value > 0f) {
                val layout = textMeasurer.measure(formatAxisValue(value), axisStyle)
                drawText(layout, topLeft = Offset(padHorizontal, y - layout.size.height - 2f))
            }
        }

        val seriesCount = series.size
        val groupWidth = slotWidth * 0.44f
        val barWidth = groupWidth / seriesCount
        val barGap = if (seriesCount > 1) barWidth * 0.12f else 0f
        val barCornerRadius = CornerRadius(
            x = (barWidth / 2f).coerceAtMost(6.dp.toPx()),
            y = (barWidth / 2f).coerceAtMost(6.dp.toPx())
        )
        val animationProgress = progress.value.coerceIn(0f, 1f)

        // --- bars
        val groupTopY = FloatArray(pointCount) { padTop + plotHeight }
        series.forEachIndexed { seriesIndex, s ->
            val brush = Brush.verticalGradient(
                colors = listOf(s.color, s.color.copy(alpha = 0.35f)),
                startY = padTop,
                endY = padTop + plotHeight
            )
            for (index in 0 until pointCount) {
                val raw = (s.values.getOrNull(index) ?: 0f).coerceIn(0f, maxVal)
                if (raw <= 0f) continue
                val barHeight = plotHeight * (raw / maxVal) * animationProgress
                val centerX = padHorizontal + slotWidth * index + slotWidth / 2f
                val left = centerX - groupWidth / 2f + seriesIndex * (barWidth + barGap)
                val top = padTop + plotHeight - barHeight
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth - barGap, barHeight),
                    cornerRadius = barCornerRadius
                )
                if (top < groupTopY[index]) {
                    groupTopY[index] = top
                }
            }
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

        // --- selection guideline and tooltip
        val selected = selectedIndex?.takeIf { it in 0 until pointCount }
        if (selected != null) {
            val anchorX = padHorizontal + slotWidth * selected + slotWidth / 2f
            drawLine(
                color = guidelineColor,
                start = Offset(anchorX, padTop),
                end = Offset(anchorX, padTop + plotHeight),
                strokeWidth = 1.5f
            )

            val tooltipEntries = series.map { s ->
                "${s.title}: ${formatValue(s.values.getOrNull(selected) ?: 0f)}" to s.color
            }
            drawChartTooltip(
                textMeasurer = textMeasurer,
                title = fullLabels.getOrNull(selected) ?: labels[selected],
                entries = tooltipEntries,
                titleStyle = tooltipTitleStyle,
                entryStyle = tooltipEntryStyle,
                anchorX = anchorX,
                anchorY = groupTopY[selected],
                surfaceColor = glass.dialogFillTop.copy(alpha = 0.96f),
                outlineColor = glass.strokeStart.copy(alpha = 0.4f),
                cornerRadius = 12.dp.toPx()
            )
        }
    }
}
