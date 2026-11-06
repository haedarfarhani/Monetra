package ir.heydar.monetra.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date formatting and timestamp manipulation.
 */
object DateUtils {

    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatPersian(timestamp: Long, pattern: String = "Y/m/d H:i"): String {
        val pDate = saman.zamani.persiandate.PersianDate(timestamp)
        val adjustedPattern = pattern
            .replace("yyyy", "Y")
            .replace("MM", "m")
            .replace("dd", "d")
            .replace("HH", "H")
            .replace("mm", "i")
            .replace("ss", "s")
        val formater = saman.zamani.persiandate.PersianDateFormat(adjustedPattern)
        return formater.format(pDate)
    }

    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
}

