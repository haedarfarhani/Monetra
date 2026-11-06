package ir.heydar.monetra.domain.model

enum class TransactionSource {
    MANUAL,
    BANK_SMS;

    companion object {
        fun fromKey(key: String?): TransactionSource =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: MANUAL
    }
}
