package ir.heydar.monetra.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens

/**
 * Glass card. When [containerColor] is null the card uses the Liquid Glass
 * translucent fill; pass a color for a solid panel (rare).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        shadowElevation = if (elevation == 0.dp) 6.dp else elevation,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            content = content
        )
    }
}

@Composable
fun MonetraCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = AppCard(
    modifier = modifier,
    elevation = elevation,
    containerColor = containerColor,
    border = border,
    onClick = onClick,
    content = content
)
