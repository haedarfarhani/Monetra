package ir.heydar.monetra.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens for the "Liquid Glass" design language (iOS-style frosted glass).
 * Every fill is translucent so the animated Aurora backdrop glows through the panel.
 */
@Immutable
data class GlassColors(
    /** Top stop of the vertical translucent fill gradient. */
    val fillTop: Color,
    /** Bottom stop of the vertical translucent fill gradient. */
    val fillBottom: Color,
    /** Bright corner of the gradient border stroke. */
    val strokeStart: Color,
    /** Subtle corner of the gradient border stroke. */
    val strokeEnd: Color,
    /** Specular sheen highlight along the top edge of the panel. */
    val sheen: Color,
    /** Soft ambient shadow cast below glass panels. */
    val shadow: Color,
    /** Denser fill for dialogs & bottom sheets sitting on a dim scrim. */
    val dialogFillTop: Color,
    val dialogFillBottom: Color,
    /** Fainter fill for elements nested inside an existing glass panel. */
    val innerFillTop: Color,
    val innerFillBottom: Color,
    /** Denser frosted fill for the floating bottom bar. */
    val barTint: Color
)

val LightGlassColors = GlassColors(
    fillTop = Color.White.copy(alpha = 0.56f),
    fillBottom = Color.White.copy(alpha = 0.30f),
    strokeStart = Color.White.copy(alpha = 0.85f),
    strokeEnd = Color.White.copy(alpha = 0.42f),
    sheen = Color.White.copy(alpha = 0.55f),
    shadow = Color(0xFF3B3F8F).copy(alpha = 0.18f),
    dialogFillTop = Color.White.copy(alpha = 0.86f),
    dialogFillBottom = Color.White.copy(alpha = 0.72f),
    innerFillTop = Color.White.copy(alpha = 0.40f),
    innerFillBottom = Color.White.copy(alpha = 0.16f),
    barTint = Color.White.copy(alpha = 0.88f)
)

val DarkGlassColors = GlassColors(
    fillTop = Color.White.copy(alpha = 0.115f),
    fillBottom = Color.White.copy(alpha = 0.045f),
    strokeStart = Color.White.copy(alpha = 0.24f),
    strokeEnd = Color.White.copy(alpha = 0.07f),
    sheen = Color.White.copy(alpha = 0.18f),
    shadow = Color.Black.copy(alpha = 0.40f),
    dialogFillTop = Color(0xFF232B45).copy(alpha = 0.90f),
    dialogFillBottom = Color(0xFF161D33).copy(alpha = 0.86f),
    innerFillTop = Color.White.copy(alpha = 0.10f),
    innerFillBottom = Color.White.copy(alpha = 0.045f),
    barTint = Color.White.copy(alpha = 0.55f)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }

val MaterialTheme.glassColors: GlassColors
    @Composable
    @ReadOnlyComposable
    get() = LocalGlassColors.current
