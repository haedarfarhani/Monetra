package ir.heydar.monetra.core.calendar

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.time.LocalDate

class JalaliCalendarTest {

    @Test
    fun `converts known gregorian dates to jalali`() {
        assertArrayEquals(intArrayOf(1403, 1, 1), JalaliCalendar.toJalali(2024, 3, 20))
        assertArrayEquals(intArrayOf(1404, 1, 1), JalaliCalendar.toJalali(2025, 3, 21))
        assertArrayEquals(intArrayOf(1357, 11, 22), JalaliCalendar.toJalali(1979, 2, 11))
    }

    @Test
    fun `converts known jalali dates to gregorian`() {
        assertArrayEquals(intArrayOf(2024, 3, 20), JalaliCalendar.toGregorian(1403, 1, 1))
        assertArrayEquals(intArrayOf(1979, 2, 11), JalaliCalendar.toGregorian(1357, 11, 22))
    }

    @Test
    fun `round trips every day of a leap gregorian year`() {
        var date = LocalDate.of(2024, 1, 1)
        while (date.year == 2024) {
            val jalali = JalaliCalendar.toJalali(date.year, date.monthValue, date.dayOfMonth)
            val back = JalaliCalendar.toGregorian(jalali[0], jalali[1], jalali[2])
            assertArrayEquals(
                "round trip failed for $date",
                intArrayOf(date.year, date.monthValue, date.dayOfMonth),
                back
            )
            date = date.plusDays(1)
        }
    }
}
