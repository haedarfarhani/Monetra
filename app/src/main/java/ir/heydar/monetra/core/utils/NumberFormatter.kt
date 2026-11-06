package ir.heydar.monetra.core.utils

import java.text.NumberFormat
import java.util.Locale

object NumberFormatter {
    fun format(value: Long, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getNumberInstance(locale).format(value)
    }

    fun formatWithPersianDigits(value: Long): String {
        val persian = Locale.forLanguageTag("fa")
        val formatted = NumberFormat.getNumberInstance(persian).format(value)
        return formatted.replace('0', '۰')
            .replace('1', '۱')
            .replace('2', '۲')
            .replace('3', '۳')
            .replace('4', '۴')
            .replace('5', '۵')
            .replace('6', '۶')
            .replace('7', '۷')
            .replace('8', '۸')
            .replace('9', '۹')
    }
}
