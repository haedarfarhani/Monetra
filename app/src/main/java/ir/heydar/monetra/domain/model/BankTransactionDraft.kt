package ir.heydar.monetra.domain.model

/** How confidently a bank message was turned into a transaction. */
enum class ParsingStatus {
    PARSED,
    NEEDS_REVIEW,
    FAILED,
    SAVED,
    REJECTED
}

/**
 * Everything a bank message can tell us. Every field except the raw message is nullable: no bank
 * message format is guaranteed to contain a reference number, a card number or a balance.
 */
data class BankTransactionDraft(
    val bankName: String? = null,
    val senderAddress: String? = null,
    val transactionType: TransactionType? = null,
    val bankTransactionType: BankTransactionType? = null,
    val amountMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val accountNumber: String? = null,
    val cardLastFourDigits: String? = null,
    val accountBalanceMinorUnits: Long? = null,
    val transactionDate: String? = null,
    val transactionTime: String? = null,
    /** Best-effort epoch millis derived from the message; null when no date was found. */
    val transactionTimestamp: Long? = null,
    val referenceNumber: String? = null,
    val description: String? = null,
    val originalMessage: String = "",
    val fingerprint: String = "",
    val confidence: Int = 0,
    val parsingStatus: ParsingStatus = ParsingStatus.NEEDS_REVIEW,
    val parsingError: String? = null
) {
    /** Only high confidence drafts may ever be saved without asking the user. */
    val isHighConfidence: Boolean
        get() = confidence >= HIGH_CONFIDENCE &&
            amountMinorUnits != null &&
            amountMinorUnits > 0 &&
            transactionType != null

    companion object {
        const val HIGH_CONFIDENCE = 80
    }
}
