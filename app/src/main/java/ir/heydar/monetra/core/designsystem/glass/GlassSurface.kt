package ir.heydar.monetra.core.designsystem.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors

/** Vertical translucent fill gradient used by glass panels. */
@Composable
fun glassFillBrush(dense: Boolean = false): Brush {
    val glass = MaterialTheme.glassColors
    return Brush.verticalGradient(
        listOf(
            if (dense) glass.dialogFillTop else glass.fillTop,
            if (dense) glass.dialogFillBottom else glass.fillBottom
        )
    )
}

/** Bright-to-subtle gradient used for glass hairline borders. */
@Composable
fun glassStrokeBrush(): Brush {
    val glass = MaterialTheme.glassColors
    return Brush.linearGradient(listOf(glass.strokeStart, glass.strokeEnd))
}

/**
 * The foundation of the Liquid Glass design language: a translucent panel with
 * a gradient fill, a bright hairline border, a specular sheen along the top edge
 * and a soft ambient shadow. The Aurora backdrop glows through the fill.
 *
 * No backdrop blur is applied here on purpose — blur is reserved for floating
 * chrome (bottom bar, FAB) where it is affordable; content panels sit on the
 * already-soft Aurora gradient.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    dense: Boolean = false,
    fill: Brush? = null,
    showStroke: Boolean = true,
    showSheen: Boolean = true,
    shadowElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = MaterialTheme.glassColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glassPressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                cameraDistance = 12f * density
            }
            .then(
                if (shadowElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = shadowElevation,
                        shape = shape,
                        clip = false,
                        ambientColor = glass.shadow,
                        spotColor = glass.shadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(fill ?: glassFillBrush(dense))
            .then(
                if (showStroke) {
                    Modifier.border(
                        border = BorderStroke(Dimens.glassStrokeWidth, glassStrokeBrush()),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (showSheen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to glass.sheen,
                                    0.25f to Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }
            )
        }
        content()
    }
}
