package ir.heydar.monetra.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class AppDateFormatterTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    private fun millisOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(utc).toInstant().toEpochMilli()

    @Test
    fun `formats jalali dates`() {
        assertEquals(
            "1403/01/01",
            AppDateFormatter.formatDate(millisOf(2024, 3, 20), CalendarSystem.JALALI, Locale.ENGLISH, utc)
        )
    }

    @Test
    fun `formats gregorian dates`() {
        assertEquals(
            "2024/03/20",
            AppDateFormatter.formatDate(millisOf(2024, 3, 20), CalendarSystem.GREGORIAN, Locale.ENGLISH, utc)
        )
    }

    @Test
    fun `shapes digits for the persian locale`() {
        @Suppress("DEPRECATION")
        val persian = Locale("fa")
        assertEquals(
            "۱۴۰۳/۰۱/۰۱",
            AppDateFormatter.formatDate(millisOf(2024, 3, 20), CalendarSystem.JALALI, persian, utc)
        )
    }

    @Test
    fun `jalali month range covers the whole jalali month`() {
        val range = AppDateFormatter.monthRange(millisOf(2024, 4, 10), CalendarSystem.JALALI, utc)
        assertEquals(millisOf(2024, 3, 20), range.first)
        assertEquals(millisOf(2024, 4, 20) - 1, range.last)
    }

    @Test
    fun `gregorian month range covers the calendar month`() {
        val range = AppDateFormatter.monthRange(millisOf(2024, 4, 10), CalendarSystem.GREGORIAN, utc)
        assertEquals(millisOf(2024, 4, 1), range.first)
        assertEquals(millisOf(2024, 5, 1) - 1, range.last)
    }
}
