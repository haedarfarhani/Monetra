package ir.heydar.monetra.core.currency

/**
 * Currencies the app can store. Amounts are kept in minor units, so the number of fraction digits
 * is part of the currency itself — a US dollar amount has cents, rials and tomans do not.
 */
enum class AppCurrency(
    val code: String,
    val fractionDigits: Int,
    val symbol: String
) {
    USD("USD", 2, "$"),
    IRR("IRR", 0, "ریال"),
    TOMAN("TOMAN", 0, "تومان");

    companion object {
        fun fromCode(code: String?): AppCurrency =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
    }
}
