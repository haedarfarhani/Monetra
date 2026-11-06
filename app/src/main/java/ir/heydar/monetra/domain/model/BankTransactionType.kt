package ir.heydar.monetra.domain.model

/**
 * Fine-grained bank transaction types extracted from bank SMS messages.
 */
enum class BankTransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
    CARD_TO_CARD,
    FEE,
    REVERSAL,
    UNKNOWN;

    fun toTransactionType(): TransactionType = when (this) {
        INCOME, TRANSFER_IN, REVERSAL -> TransactionType.INCOME
        EXPENSE, TRANSFER_OUT, CARD_TO_CARD, FEE -> TransactionType.EXPENSE
        UNKNOWN -> TransactionType.EXPENSE
    }
}
