package ir.heydar.monetra.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.calendar.PersianDigits
import java.time.LocalTime
import java.util.Locale

private enum class TimePickerFocus {
    HOUR,
    MINUTE
}

@Composable
fun MonetraTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    locale: Locale = Locale.getDefault(),
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var focusedField by remember { mutableStateOf(TimePickerFocus.HOUR) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .widthIn(max = 350.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            dense = true,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.time_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Digital Cards for Hour and Minute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Box
                    TimeDisplayCard(
                        value = selectedHour,
                        label = stringResource(id = R.string.hour_label),
                        isSelected = focusedField == TimePickerFocus.HOUR,
                        locale = locale,
                        onClick = { focusedField = TimePickerFocus.HOUR }
                    )

                    // Colon Separator
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minute Box
                    TimeDisplayCard(
                        value = selectedMinute,
                        label = stringResource(id = R.string.minute_label),
                        isSelected = focusedField == TimePickerFocus.MINUTE,
                        locale = locale,
                        onClick = { focusedField = TimePickerFocus.MINUTE }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Buttons (+ / -) for the active field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (focusedField == TimePickerFocus.HOUR) {
                                selectedHour = (selectedHour - 1 + 24) % 24
                            } else {
                                selectedMinute = (selectedMinute - 1 + 60) % 60
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = if (focusedField == TimePickerFocus.HOUR) {
                            stringResource(id = R.string.hour_label)
                        } else {
                            stringResource(id = R.string.minute_label)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    FilledIconButton(
                        onClick = {
                            if (focusedField == TimePickerFocus.HOUR) {
                                selectedHour = (selectedHour + 1) % 24
                            } else {
                                selectedMinute = (selectedMinute + 1) % 60
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Number Grid for Selection
                AnimatedContent(
                    targetState = focusedField,
                    label = "TimeGridAnim"
                ) { field ->
                    when (field) {
                        TimePickerFocus.HOUR -> {
                            HourGridView(
                                selectedHour = selectedHour,
                                locale = locale,
                                onSelectHour = {
                                    selectedHour = it
                                    focusedField = TimePickerFocus.MINUTE
                                }
                            )
                        }
                        TimePickerFocus.MINUTE -> {
                            MinuteGridView(
                                selectedMinute = selectedMinute,
                                locale = locale,
                                onSelectMinute = { selectedMinute = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Now" Shortcut
                    TextButton(
                        onClick = {
                            val now = LocalTime.now()
                            selectedHour = now.hour
                            selectedMinute = now.minute
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.time_now),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(id = R.string.action_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MonetraPrimaryButton(
                            text = stringResource(id = R.string.action_confirm),
                            onClick = {
                                onTimeSelected(selectedHour, selectedMinute)
                                onDismiss()
                            },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeDisplayCard(
    value: Int,
    label: String,
    isSelected: Boolean,
    locale: Locale,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "TimeDisplayBgAnim"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "TimeDisplayBorderAnim"
    )

    val formattedNumber = remember(value, locale) {
        val str = "%02d".format(value)
        if (locale.language == "fa") PersianDigits.toPersian(str) else str
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = formattedNumber,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HourGridView(
    selectedHour: Int,
    locale: Locale,
    onSelectHour: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(24) { hour ->
            val isSelected = hour == selectedHour
            val hourText = remember(hour, locale) {
                val str = "%02d".format(hour)
                if (locale.language == "fa") PersianDigits.toPersian(str) else str
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .clickable { onSelectHour(hour) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hourText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MinuteGridView(
    selectedMinute: Int,
    locale: Locale,
    onSelectMinute: (Int) -> Unit
) {
    // 5-minute intervals: 00, 05, 10, 15, ..., 55
    val minutePresets = remember { (0 until 60 step 5).toList() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(minutePresets) { minute ->
            val isSelected = (selectedMinute == minute)
            val minuteText = remember(minute, locale) {
                val str = "%02d".format(minute)
                if (locale.language == "fa") PersianDigits.toPersian(str) else str
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectMinute(minute) }
            ) {
                Text(
                    text = minuteText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}
