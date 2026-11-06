package ir.heydar.monetra.core.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Presentation-only date formatting. Every input is an epoch-millis timestamp, so the selected
 * [CalendarSystem] affects how a date is *shown* and which day range a "month" covers, never how
 * it is stored.
 */
object AppDateFormatter {

    private const val DATE_PATTERN = "yyyy/MM/dd"

    fun formatDate(
        timestamp: Long,
        calendar: CalendarSystem,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        return when (calendar) {
            CalendarSystem.GREGORIAN -> date.format(DateTimeFormatter.ofPattern(DATE_PATTERN, locale))
            CalendarSystem.JALALI -> {
                val jalali = JalaliCalendar.toJalali(date.year, date.monthValue, date.dayOfMonth)
                val text = "%04d/%02d/%02d".format(jalali[0], jalali[1], jalali[2])
                if (locale.language == "fa") PersianDigits.toPersian(text) else text
            }
        }
    }

    fun formatDateTime(
        timestamp: Long,
        calendar: CalendarSystem,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val time = Instant.ofEpochMilli(timestamp).atZone(zone)
        val hourMinute = "%02d:%02d".format(time.hour, time.minute)
        val localizedTime = if (locale.language == "fa") PersianDigits.toPersian(hourMinute) else hourMinute
        return "${formatDate(timestamp, calendar, locale, zone)} $localizedTime"
    }

    /**
     * Inclusive millisecond range of the month that contains [timestamp], expressed in the given
     * calendar. A Jalali month starts on a different Gregorian day, so range queries must use this
     * instead of [java.util.Calendar] month arithmetic.
     */
    fun monthRange(
        timestamp: Long,
        calendar: CalendarSystem,
        zone: ZoneId = ZoneId.systemDefault()
    ): LongRange {
        val startDate: LocalDate
        val endExclusive: LocalDate
        when (calendar) {
            CalendarSystem.GREGORIAN -> {
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                startDate = date.withDayOfMonth(1)
                endExclusive = startDate.plusMonths(1)
            }
            CalendarSystem.JALALI -> {
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                val jalali = JalaliCalendar.toJalali(date.year, date.monthValue, date.dayOfMonth)
                val year = jalali[0]
                val month = jalali[1]
                val nextYear = if (month == 12) year + 1 else year
                val nextMonth = if (month == 12) 1 else month + 1

                val startGregorian = JalaliCalendar.toGregorian(year, month, 1)
                val endGregorian = JalaliCalendar.toGregorian(nextYear, nextMonth, 1)

                startDate = LocalDate.of(startGregorian[0], startGregorian[1], startGregorian[2])
                endExclusive = LocalDate.of(endGregorian[0], endGregorian[1], endGregorian[2])
            }
        }

        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMillis..endMillis
    }

    /**
     * Inclusive millisecond range of the whole year that contains [timestamp], expressed in the
     * given calendar. A Jalali year starts on a different Gregorian day, so range queries must use
     * this instead of Gregorian year arithmetic.
     */
    fun yearRange(
        timestamp: Long,
        calendar: CalendarSystem,
        zone: ZoneId = ZoneId.systemDefault()
    ): LongRange {
        val startDate: LocalDate
        val endExclusive: LocalDate
        when (calendar) {
            CalendarSystem.GREGORIAN -> {
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                startDate = date.withDayOfYear(1)
                endExclusive = startDate.plusYears(1)
            }
            CalendarSystem.JALALI -> {
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                val jalali = JalaliCalendar.toJalali(date.year, date.monthValue, date.dayOfMonth)
                val year = jalali[0]

                val startGregorian = JalaliCalendar.toGregorian(year, 1, 1)
                val endGregorian = JalaliCalendar.toGregorian(year + 1, 1, 1)

                startDate = LocalDate.of(startGregorian[0], startGregorian[1], startGregorian[2])
                endExclusive = LocalDate.of(endGregorian[0], endGregorian[1], endGregorian[2])
            }
        }

        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMillis..endMillis
    }
}

/** Shapes Latin digits into Persian digits for display (storage stays Latin). */
object PersianDigits {
    private val mapping = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersian(text: String): String = buildString(text.length) {
        text.forEach { char ->
            if (char in '0'..'9') append(mapping[char - '0']) else append(char)
        }
    }
}
