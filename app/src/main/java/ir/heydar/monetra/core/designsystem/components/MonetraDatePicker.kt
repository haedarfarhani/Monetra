package ir.heydar.monetra.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.calendar.PersianDigits
import saman.zamani.persiandate.PersianDate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private val PERSIAN_MONTH_NAMES = listOf(
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
)

private val GREGORIAN_MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private val PERSIAN_WEEK_DAYS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
private val GREGORIAN_WEEK_DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

private enum class PickerViewMode {
    DAYS_GRID,
    YEAR_MONTH_PICKER
}

@Composable
fun MonetraDatePickerDialog(
    initialTimestamp: Long,
    initialCalendar: CalendarSystem = CalendarSystem.JALALI,
    locale: Locale = Locale.getDefault(),
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var calendarSystem by remember { mutableStateOf(initialCalendar) }
    var selectedTimestamp by remember { mutableStateOf(initialTimestamp) }
    var viewMode by remember { mutableStateOf(PickerViewMode.DAYS_GRID) }

    // Jalali view coordinates
    val initialPDate = remember(selectedTimestamp) { PersianDate(selectedTimestamp) }
    var jalaliYear by remember { mutableIntStateOf(initialPDate.shYear) }
    var jalaliMonth by remember { mutableIntStateOf(initialPDate.shMonth) }

    // Gregorian view coordinates
    val initialGDate = remember(selectedTimestamp) {
        Instant.ofEpochMilli(selectedTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var grgYear by remember { mutableIntStateOf(initialGDate.year) }
    var grgMonth by remember { mutableIntStateOf(initialGDate.monthValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
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
                // Header: Title & Calendar Selector Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.date_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Calendar System Toggle
                    CalendarToggleChip(
                        currentCalendar = calendarSystem,
                        onCalendarChanged = { newCal ->
                            calendarSystem = newCal
                            // Sync view year/month with current selected timestamp
                            if (newCal == CalendarSystem.JALALI) {
                                val p = PersianDate(selectedTimestamp)
                                jalaliYear = p.shYear
                                jalaliMonth = p.shMonth
                            } else {
                                val g = Instant.ofEpochMilli(selectedTimestamp)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                grgYear = g.year
                                grgMonth = g.monthValue
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Date Formatted Badge
                SelectedDateDisplay(
                    timestamp = selectedTimestamp,
                    calendar = calendarSystem,
                    locale = locale
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Month / Year Navigation Bar
                MonthYearNavBar(
                    calendar = calendarSystem,
                    jalaliYear = jalaliYear,
                    jalaliMonth = jalaliMonth,
                    grgYear = grgYear,
                    grgMonth = grgMonth,
                    viewMode = viewMode,
                    locale = locale,
                    onToggleViewMode = {
                        viewMode = if (viewMode == PickerViewMode.DAYS_GRID) {
                            PickerViewMode.YEAR_MONTH_PICKER
                        } else {
                            PickerViewMode.DAYS_GRID
                        }
                    },
                    onPrevious = {
                        if (calendarSystem == CalendarSystem.JALALI) {
                            if (jalaliMonth == 1) {
                                jalaliMonth = 12
                                jalaliYear -= 1
                            } else {
                                jalaliMonth -= 1
                            }
                        } else {
                            if (grgMonth == 1) {
                                grgMonth = 12
                                grgYear -= 1
                            } else {
                                grgMonth -= 1
                            }
                        }
                    },
                    onNext = {
                        if (calendarSystem == CalendarSystem.JALALI) {
                            if (jalaliMonth == 12) {
                                jalaliMonth = 1
                                jalaliYear += 1
                            } else {
                                jalaliMonth += 1
                            }
                        } else {
                            if (grgMonth == 12) {
                                grgMonth = 1
                                grgYear += 1
                            } else {
                                grgMonth += 1
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content: Days Grid OR Year-Month Picker
                AnimatedContent(
                    targetState = viewMode,
                    label = "PickerViewModeAnim"
                ) { mode ->
                    when (mode) {
                        PickerViewMode.DAYS_GRID -> {
                            DaysGridView(
                                calendar = calendarSystem,
                                jalaliYear = jalaliYear,
                                jalaliMonth = jalaliMonth,
                                grgYear = grgYear,
                                grgMonth = grgMonth,
                                selectedTimestamp = selectedTimestamp,
                                locale = locale,
                                onSelectDate = { newTimestamp ->
                                    selectedTimestamp = newTimestamp
                                }
                            )
                        }
                        PickerViewMode.YEAR_MONTH_PICKER -> {
                            YearMonthPickerView(
                                calendar = calendarSystem,
                                jalaliYear = jalaliYear,
                                jalaliMonth = jalaliMonth,
                                grgYear = grgYear,
                                grgMonth = grgMonth,
                                locale = locale,
                                onSelectMonthYear = { y, m ->
                                    if (calendarSystem == CalendarSystem.JALALI) {
                                        jalaliYear = y
                                        jalaliMonth = m
                                    } else {
                                        grgYear = y
                                        grgMonth = m
                                    }
                                    viewMode = PickerViewMode.DAYS_GRID
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Today Shortcut
                    TextButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            selectedTimestamp = now
                            if (calendarSystem == CalendarSystem.JALALI) {
                                val p = PersianDate(now)
                                jalaliYear = p.shYear
                                jalaliMonth = p.shMonth
                            } else {
                                val g = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
                                grgYear = g.year
                                grgMonth = g.monthValue
                            }
                            viewMode = PickerViewMode.DAYS_GRID
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.date_today),
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
                                onDateSelected(selectedTimestamp)
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
private fun CalendarToggleChip(
    currentCalendar: CalendarSystem,
    onCalendarChanged: (CalendarSystem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isJalali = currentCalendar == CalendarSystem.JALALI

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isJalali) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onCalendarChanged(CalendarSystem.JALALI) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.calendar_jalali).substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isJalali) FontWeight.Bold else FontWeight.Medium,
                    color = if (isJalali) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (!isJalali) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onCalendarChanged(CalendarSystem.GREGORIAN) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.calendar_gregorian),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (!isJalali) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isJalali) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectedDateDisplay(
    timestamp: Long,
    calendar: CalendarSystem,
    locale: Locale
) {
    val dateText = remember(timestamp, calendar, locale) {
        if (calendar == CalendarSystem.JALALI) {
            val pDate = PersianDate(timestamp)
            val monthName = PERSIAN_MONTH_NAMES.getOrNull(pDate.shMonth - 1).orEmpty()
            val text = "${pDate.dayName()}، ${pDate.shDay} $monthName ${pDate.shYear}"
            if (locale.language == "fa") PersianDigits.toPersian(text) else text
        } else {
            val lDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            val monthName = GREGORIAN_MONTH_NAMES.getOrNull(lDate.monthValue - 1).orEmpty()
            "${lDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, $monthName ${lDate.dayOfMonth}, ${lDate.year}"
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        )
    }
}

@Composable
private fun MonthYearNavBar(
    calendar: CalendarSystem,
    jalaliYear: Int,
    jalaliMonth: Int,
    grgYear: Int,
    grgMonth: Int,
    viewMode: PickerViewMode,
    locale: Locale,
    onToggleViewMode: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val titleText = remember(calendar, jalaliYear, jalaliMonth, grgYear, grgMonth, locale) {
        if (calendar == CalendarSystem.JALALI) {
            val monthName = PERSIAN_MONTH_NAMES.getOrNull(jalaliMonth - 1).orEmpty()
            val text = "$monthName $jalaliYear"
            if (locale.language == "fa") PersianDigits.toPersian(text) else text
        } else {
            val monthName = GREGORIAN_MONTH_NAMES.getOrNull(grgMonth - 1).orEmpty()
            "$monthName $grgYear"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleViewMode() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (viewMode == PickerViewMode.YEAR_MONTH_PICKER) {
                    Icons.Default.ArrowDropUp
                } else {
                    Icons.Default.ArrowDropDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DaysGridView(
    calendar: CalendarSystem,
    jalaliYear: Int,
    jalaliMonth: Int,
    grgYear: Int,
    grgMonth: Int,
    selectedTimestamp: Long,
    locale: Locale,
    onSelectDate: (Long) -> Unit
) {
    val weekDays = if (calendar == CalendarSystem.JALALI) PERSIAN_WEEK_DAYS else GREGORIAN_WEEK_DAYS

    // Calculate days count and first day offset
    val (firstDayOffset, daysInMonth) = remember(calendar, jalaliYear, jalaliMonth, grgYear, grgMonth) {
        if (calendar == CalendarSystem.JALALI) {
            val pDate = PersianDate().initJalaliDate(jalaliYear, jalaliMonth, 1)
            val offset = pDate.dayOfWeek() // 0 = Shanbe .. 6 = Jomeh
            val daysCount = pDate.monthDays
            offset to daysCount
        } else {
            val date = LocalDate.of(grgYear, grgMonth, 1)
            val offset = date.dayOfWeek.value - 1 // 0 = Mon .. 6 = Sun
            val daysCount = YearMonth.of(grgYear, grgMonth).lengthOfMonth()
            offset to daysCount
        }
    }

    // Determine current selected day in this view
    val isCurrentMonthView = remember(calendar, jalaliYear, jalaliMonth, grgYear, grgMonth, selectedTimestamp) {
        if (calendar == CalendarSystem.JALALI) {
            val p = PersianDate(selectedTimestamp)
            p.shYear == jalaliYear && p.shMonth == jalaliMonth
        } else {
            val g = Instant.ofEpochMilli(selectedTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            g.year == grgYear && g.monthValue == grgMonth
        }
    }
    val selectedDay = remember(calendar, selectedTimestamp) {
        if (calendar == CalendarSystem.JALALI) {
            PersianDate(selectedTimestamp).shDay
        } else {
            Instant.ofEpochMilli(selectedTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
        }
    }

    // Determine today day in this view
    val todayMillis = remember { System.currentTimeMillis() }
    val isTodayMonthView = remember(calendar, jalaliYear, jalaliMonth, grgYear, grgMonth, todayMillis) {
        if (calendar == CalendarSystem.JALALI) {
            val p = PersianDate(todayMillis)
            p.shYear == jalaliYear && p.shMonth == jalaliMonth
        } else {
            val g = Instant.ofEpochMilli(todayMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            g.year == grgYear && g.monthValue == grgMonth
        }
    }
    val todayDay = remember(calendar, todayMillis) {
        if (calendar == CalendarSystem.JALALI) {
            PersianDate(todayMillis).shDay
        } else {
            Instant.ofEpochMilli(todayMillis).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Week Days Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid (Up to 6 rows x 7 cols = 42 cells)
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNumber = cellIndex - firstDayOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val isSelected = isCurrentMonthView && (dayNumber == selectedDay)
                        val isToday = isTodayMonthView && (dayNumber == todayDay)

                        val dayText = if (locale.language == "fa" || calendar == CalendarSystem.JALALI) {
                            PersianDigits.toPersian(dayNumber.toString())
                        } else {
                            dayNumber.toString()
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable {
                                    val zone = ZoneId.systemDefault()
                                    val newMillis = if (calendar == CalendarSystem.JALALI) {
                                        val grg = PersianDate().jalali_to_gregorian(jalaliYear, jalaliMonth, dayNumber)
                                        LocalDate.of(grg[0], grg[1], grg[2])
                                            .atStartOfDay(zone)
                                            .toInstant()
                                            .toEpochMilli()
                                    } else {
                                        LocalDate.of(grgYear, grgMonth, dayNumber)
                                            .atStartOfDay(zone)
                                            .toInstant()
                                            .toEpochMilli()
                                    }
                                    onSelectDate(newMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    } else {
                        // Empty cell for alignment
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun YearMonthPickerView(
    calendar: CalendarSystem,
    jalaliYear: Int,
    jalaliMonth: Int,
    grgYear: Int,
    grgMonth: Int,
    locale: Locale,
    onSelectMonthYear: (year: Int, month: Int) -> Unit
) {
    var selectedYear by remember(calendar, jalaliYear, grgYear) {
        mutableIntStateOf(if (calendar == CalendarSystem.JALALI) jalaliYear else grgYear)
    }

    val monthNames = if (calendar == CalendarSystem.JALALI) PERSIAN_MONTH_NAMES else GREGORIAN_MONTH_NAMES
    val currentMonth = if (calendar == CalendarSystem.JALALI) jalaliMonth else grgMonth

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Year Stepper Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedYear -= 1 }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Year",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            val yearText = if (locale.language == "fa" || calendar == CalendarSystem.JALALI) {
                PersianDigits.toPersian(selectedYear.toString())
            } else {
                selectedYear.toString()
            }

            Text(
                text = yearText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = { selectedYear += 1 }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Year",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 12 Months Grid (4 rows x 3 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(12) { index ->
                val monthNumber = index + 1
                val isSelected = (monthNumber == currentMonth)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMonthYear(selectedYear, monthNumber) }
                ) {
                    Text(
                        text = monthNames[index],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
