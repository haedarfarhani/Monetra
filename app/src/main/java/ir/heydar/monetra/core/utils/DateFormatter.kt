package ir.heydar.monetra.core.utils

import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    fun formatGregorian(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", locale)
        return sdf.format(Date(timestamp))
    }

    fun formatPersian(timestamp: Long): String {
        val pDate = PersianDate(timestamp)
        val formater = PersianDateFormat("Y/m/d")
        return formater.format(pDate)
    }
}
