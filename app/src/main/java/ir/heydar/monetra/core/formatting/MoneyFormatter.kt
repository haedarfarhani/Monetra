package ir.heydar.monetra.core.formatting

import ir.heydar.monetra.core.currency.AppCurrency
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Formats amounts held in minor units without ever touching floating point arithmetic.
 *
 * `1_234_56` (minor units) with [AppCurrency.USD] renders as `1234.56 $` using the locale's
 * grouping and digit shapes; with [AppCurrency.IRR] the value is shown as-is because rials have no
 * sub-unit.
 */
object MoneyFormatter {

    fun format(
        amountMinorUnits: Long,
        currency: AppCurrency,
        locale: Locale = Locale.getDefault()
    ): String {
        val sign = if (amountMinorUnits < 0) "-" else ""
        val absolute = abs(amountMinorUnits)
        val factor = pow10(currency.fractionDigits)

        val wholePart = absolute / factor
        val fractionPart = absolute % factor

        val wholeText = NumberFormat.getIntegerInstance(locale).format(wholePart)
        val numberText = if (currency.fractionDigits == 0) {
            wholeText
        } else {
            val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
            wholeText + separator + fractionPart.toString().padStart(currency.fractionDigits, '0')
        }

        return "$sign$numberText ${currency.symbol}"
    }

    /** Number-only rendering (no currency symbol), still minor-unit aware. */
    fun formatNumber(
        amountMinorUnits: Long,
        currency: AppCurrency,
        locale: Locale = Locale.getDefault()
    ): String = format(amountMinorUnits, currency, locale).substringBefore(' ')

    /** Converts a user-typed major-unit decimal string into minor units, or null when invalid. */
    fun parseToMinorUnits(input: String, currency: AppCurrency): Long? {
        val normalized = input.trim()
            .replace(",", "")
            .replace("،", "")
            .replace("\u066B", ".")
        if (normalized.isEmpty()) return null

        val sign = if (normalized.startsWith("-")) -1 else 1
        val unsigned = normalized.removePrefix("-").removePrefix("+")
        if (unsigned.isEmpty()) return null

        val parts = unsigned.split('.')
        if (parts.size > 2) return null
        val wholePart = parts[0].ifEmpty { "0" }
        if (!wholePart.all { it.isDigit() }) return null

        val fractionDigits = currency.fractionDigits
        val fractionPart = parts.getOrNull(1).orEmpty()
        if (fractionPart.any { !it.isDigit() }) return null
        if (fractionPart.length > fractionDigits) return null

        val whole = wholePart.toLongOrNull() ?: return null
        val paddedFraction = fractionPart.padEnd(fractionDigits, '0')
        val fraction = if (fractionDigits == 0) 0L else paddedFraction.toLongOrNull() ?: return null

        return sign * (whole * pow10(fractionDigits) + fraction)
    }

    private fun pow10(exponent: Int): Long {
        var result = 1L
        repeat(exponent) { result *= 10 }
        return result
    }
}
