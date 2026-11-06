package ir.heydar.monetra.core.calendar

import saman.zamani.persiandate.PersianDate

/**
 * Gregorian <-> Jalali conversion using PersianDate library (com.github.samanzamani:PersianDate).
 */
object JalaliCalendar {

    /** @return `[jalaliYear, jalaliMonth, jalaliDay]`, month and day are 1-based. */
    fun toJalali(gregorianYear: Int, gregorianMonth: Int, gregorianDay: Int): IntArray {
        return PersianDate().gregorian_to_jalali(gregorianYear, gregorianMonth, gregorianDay)
    }

    /** @return `[gregorianYear, gregorianMonth, gregorianDay]`, month and day are 1-based. */
    fun toGregorian(jalaliYear: Int, jalaliMonth: Int, jalaliDay: Int): IntArray {
        return PersianDate().jalali_to_gregorian(jalaliYear, jalaliMonth, jalaliDay)
    }
}
