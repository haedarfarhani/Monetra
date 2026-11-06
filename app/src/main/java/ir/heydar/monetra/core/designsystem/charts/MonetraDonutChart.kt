package ir.heydar.monetra.core.designsystem.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.theme.glassColors
import kotlin.math.atan2
import kotlin.math.sqrt

/** One wedge of the donut chart. */
data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color
)

/**
 * Animated donut chart for category breakdowns. Tapping a wedge selects it (the wedge pops out,
 * others dim); [centerContent] is overlaid in the hole so the caller can render the total or the
 * selected slice's details.
 */
@Composable
fun MonetraDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    selectedSlice: Int? = null,
    onSliceSelected: (Int?) -> Unit = {},
    centerContent: @Composable () -> Unit = {}
) {
    val glass = MaterialTheme.glassColors
    val progress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices, selectedSlice) {
                    detectTapGestures(
                        onTap = { offset ->
                            val total = slices.sumOf { it.value.toDouble() }.toFloat()
                            if (total <= 0f) return@detectTapGestures

                            val strokePx = strokeWidth.toPx()
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = (minOf(size.width, size.height).toFloat() - strokePx) / 2f

                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)
                            if (distance < radius - strokePx * 1.3f || distance > radius + strokePx) {
                                return@detectTapGestures
                            }

                            // Convert tap angle to 0..360 measured from the top of the donut.
                            var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
                            if (angle < 0f) angle += 360f

                            var acc = 0f
                            var found: Int? = null
                            slices.forEachIndexed { index, slice ->
                                val sliceSweep = (slice.value / total) * 360f
                                if (found == null && angle < acc + sliceSweep) found = index
                                acc += sliceSweep
                            }
                            // Toggle off when tapping the already-selected slice.
                            onSliceSelected(if (found == selectedSlice) null else found)
                        }
                    )
                }
        ) {
            val strokePx = strokeWidth.toPx()
            val total = slices.sumOf { it.value.toDouble() }.toFloat()

            if (total <= 0f) {
                // Neutral full ring when there is nothing to break down yet.
                drawCircle(
                    color = glass.innerFillTop,
                    radius = (size.minDimension - strokePx) / 2f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = strokePx)
                )
                return@Canvas
            }

            val ringSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
            val topLeft = Offset(
                (size.width - ringSize.width) / 2f,
                (size.height - ringSize.height) / 2f
            )

            val gapDegrees = if (slices.size > 1) 3f else 0f
            val animation = progress.value
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val rawSweep = (slice.value / total) * 360f * animation
                val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)
                if (sweep <= 0f) return@forEachIndexed

                val isSelected = selectedSlice == index
                val dimmed = selectedSlice != null && !isSelected
                val color = if (dimmed) slice.color.copy(alpha = 0.3f) else slice.color

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = ringSize,
                    style = Stroke(width = if (isSelected) strokePx * 1.18f else strokePx, cap = StrokeCap.Butt)
                )
                startAngle += rawSweep
            }
        }

        centerContent()
    }
}
