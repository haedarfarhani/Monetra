package ir.heydar.monetra.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.monetraColors

/**
 * Hero balance card with a rich brand gradient, specular highlights and a
 * glass rim — the primary visual anchor of the Liquid Glass dashboard.
 *
 * When [savingsRate] is provided a "savings rate" progress section is rendered,
 * and each entry of [infoChips] becomes a frosted pill on the bottom row.
 */
@Composable
fun MonetraBalanceCard(
    totalBalanceFormatted: String,
    monthlyIncomeFormatted: String,
    monthlyExpenseFormatted: String,
    currencyLabel: String,
    modifier: Modifier = Modifier,
    savingsRate: Float? = null,
    savingsRateLabel: String? = null,
    infoChips: List<String> = emptyList()
) {
    val extendedColors = MaterialTheme.monetraColors
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            extendedColors.balanceGradientStart,
            extendedColors.balanceGradientMid,
            extendedColors.balanceGradientEnd
        ),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        fill = gradientBrush,
        showStroke = true,
        showSheen = true
    ) {
        // Soft light blobs keep the gradient from looking flat without heavy decoration.
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 1.02f, size.height * 0.02f),
                    radius = size.width * 0.45f
                ),
                radius = size.width * 0.45f,
                center = Offset(size.width * 1.02f, size.height * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 1.05f),
                    radius = size.width * 0.35f
                ),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.05f, size.height * 1.05f)
            )
        }

        Column(
            modifier = Modifier.padding(
                horizontal = Dimens.spacingLg,
                vertical = Dimens.spacingXl
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.total_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.80f)
                )
                if (currencyLabel.isNotBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = currencyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            Text(
                text = totalBalanceFormatted,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                BalanceMiniStat(
                    label = stringResource(id = R.string.monthly_income),
                    formattedAmount = monthlyIncomeFormatted,
                    icon = Icons.Default.ArrowUpward,
                    iconTint = Color(0xFF9FE7C4),
                    modifier = Modifier.weight(1f)
                )
                BalanceMiniStat(
                    label = stringResource(id = R.string.monthly_expense),
                    formattedAmount = monthlyExpenseFormatted,
                    icon = Icons.Default.ArrowDownward,
                    iconTint = Color(0xFFF5A9AD),
                    modifier = Modifier.weight(1f)
                )
            }

            if (savingsRate != null) {
                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                val animatedRate by animateFloatAsState(
                    targetValue = savingsRate.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    label = "savingsRate"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = savingsRateLabel.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text = "${(savingsRate.coerceIn(0f, 1f) * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Frosted progress track with a bright fill.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedRate)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                    )
                }
            }

            if (infoChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacingMd))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    infoChips.forEach { chip ->
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceMiniStat(
    label: String,
    formattedAmount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
