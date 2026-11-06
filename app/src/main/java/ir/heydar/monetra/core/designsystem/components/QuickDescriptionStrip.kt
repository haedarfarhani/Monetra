package ir.heydar.monetra.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.theme.Dimens

@Composable
fun QuickDescriptionChip(
    labelResId: Int,
    localizedLabel: String,
    isCustom: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(localizedLabel) }
            .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isCustom) {
                if (localizedLabel.isNotEmpty()) {
                    Icons.Filled.Add
                } else {
                    Icons.Filled.Check
                }
            } else {
                Icons.Filled.Add
            },
            contentDescription = null,
            tint = if (isCustom) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = localizedLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCustom) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun QuickDescriptionChips(
    chips: List<QuickDescriptionChip>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            QuickDescriptionChip(
                labelResId = chip.labelResId,
                localizedLabel = chip.localizedLabel,
                isCustom = chip.labelResId == 0,
                onClick = { onSelect(chip.localizedLabel) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
