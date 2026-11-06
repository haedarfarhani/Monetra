package ir.heydar.monetra.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors

/** UI-only data for one quick-description chip. */
data class QuickDescriptionChip(
    val labelResId: Int,
    val localizedLabel: String,
) {
    override fun equals(other: Any?): Boolean =
        other is QuickDescriptionChip && labelResId == other.labelResId

    override fun hashCode(): Int = labelResId.hashCode()
}

/**
 * Scrollable horizontal strip of single-line description chips.
 *
 * Each chip occupies one line; the strip scrolls horizontally so the
 * descriptions never crowd the screen. Selecting a chip sets the
 * description field.
 */
@Composable
fun QuickDescriptionStrip(
    modifier: Modifier = Modifier,
    items: List<QuickDescriptionChip>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
    chipHeight: Dp = 34.dp,
    spacing: Dp = 8.dp,
    listHorizontalPadding: Dp = Dimens.screenPadding,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = listHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.transaction_quick_description),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.OpenInNew,
                    contentDescription = if (expanded) {
                        stringResource(R.string.transaction_quick_close)
                    } else {
                        stringResource(R.string.transaction_quick_show)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (expanded) {
            val listState = rememberLazyListState()
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = listHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(items, key = { it.labelResId }) { chip ->
                    val isSelected = selectedLabel == chip.localizedLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(chip.localizedLabel) },
                        label = {
                            Text(
                                text = chip.localizedLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.glassColors.innerFillTop,
                            labelColor = MaterialTheme.colorScheme.onBackground,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            selectedContainerColor =
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.height(chipHeight),
                    )
                }
            }
        }
    }
}
