package ir.heydar.monetra.core.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Long, currencyCode: String = "USD", locale: Locale = Locale.getDefault()): String {
        val fmt = NumberFormat.getCurrencyInstance(locale)
        // Keep amount as long; format via currency symbol format
        // For simplicity use formatted number with symbol appended
        val numberStr = NumberFormat.getNumberInstance(locale).format(amount)
        return "$currencyCode $numberStr"
    }

    fun formatLocalized(amount: Long, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getCurrencyInstance(locale).format(amount / 100.0)
    }
}
