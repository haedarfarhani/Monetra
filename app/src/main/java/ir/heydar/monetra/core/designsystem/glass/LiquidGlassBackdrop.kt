package ir.heydar.monetra.core.designsystem.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ir.heydar.monetra.core.designsystem.theme.AuroraBlobsDark
import ir.heydar.monetra.core.designsystem.theme.AuroraBlobsLight
import ir.heydar.monetra.core.designsystem.theme.AuroraBaseDark
import ir.heydar.monetra.core.designsystem.theme.AuroraBaseLight
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen animated Aurora backdrop that every glass panel refracts.
 * Soft radial colour blobs drift slowly for the "liquid" feel. The blob edges
 * are radial-gradient fades, so it renders smoothly on every supported API
 * level without needing RenderEffect.
 */
@Composable
fun LiquidGlassBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = if (dark) AuroraBaseDark else AuroraBaseLight
    val blobs = if (dark) AuroraBlobsDark else AuroraBlobsLight
    val blobAlpha = if (dark) 0.95f else 0.65f

    // One full sweep of the phase circle; integer blob speeds keep the loop seamless.
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing)
        ),
        label = "auroraPhase"
    )

    // Spacer gives the drawBehind modifier a real layout node — without it
    // nothing is ever emitted and the backdrop stays invisible.
    Spacer(
        modifier.drawBehind {
            drawRect(base)
            AURORA_BLOBS.forEachIndexed { index, spec ->
                if (index >= blobs.size) return@forEachIndexed
                val color = blobs[index]
                val angle = phase * spec.speed + spec.phase
                val centerX = size.width * (spec.x + spec.dx * sin(angle))
                val centerY = size.height * (spec.y + spec.dy * cos(angle * 0.7f + spec.phase))
                val radius = size.minDimension * spec.radius
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = blobAlpha),
                            color.copy(alpha = blobAlpha * 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }
        }
    )
}

/** Layout-independent drift parameters for each aurora blob. */
private data class AuroraBlobSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val dx: Float,
    val dy: Float,
    /** Integer multiplier of the base phase so the infinite loop stays seamless. */
    val speed: Float,
    val phase: Float
)

private val AURORA_BLOBS = listOf(
    AuroraBlobSpec(x = 0.15f, y = 0.05f, radius = 0.45f, dx = 0.10f, dy = 0.08f, speed = 1f, phase = 0.0f),
    AuroraBlobSpec(x = 0.90f, y = 0.12f, radius = 0.40f, dx = 0.08f, dy = 0.10f, speed = 2f, phase = 1.7f),
    AuroraBlobSpec(x = 0.10f, y = 0.85f, radius = 0.42f, dx = 0.11f, dy = 0.09f, speed = 1f, phase = 3.4f),
    AuroraBlobSpec(x = 0.85f, y = 0.90f, radius = 0.38f, dx = 0.09f, dy = 0.11f, speed = 3f, phase = 4.6f),
    AuroraBlobSpec(x = 0.50f, y = 0.45f, radius = 0.48f, dx = 0.06f, dy = 0.07f, speed = 2f, phase = 5.5f)
)
