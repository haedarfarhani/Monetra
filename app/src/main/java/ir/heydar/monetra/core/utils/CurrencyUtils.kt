package ir.heydar.monetra.core.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Utility functions for currency formatting. Amounts are stored as Long (in cents or main currency units).
 */
object CurrencyUtils {

    fun formatCurrency(amount: Long, currencyCode: String = "USD"): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        try {
            format.currency = java.util.Currency.getInstance(currencyCode)
        } catch (_: Exception) {
            // Fallback if currency code is custom or invalid
        }
        return format.format(amount)
    }

    fun formatAmount(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        return format.format(amount)
    }
}

