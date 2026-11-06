package ir.heydar.monetra.domain.model

/**
 * Domain model representing an extracted bank transaction.
 */
data class BankTransaction(
    val id: Long = 0L,
    val bankName: String? = null,
    val senderAddress: String? = null,
    val transactionType: BankTransactionType = BankTransactionType.UNKNOWN,
    val amountMinorUnits: Long,
    val currencyCode: String = "IRR",
    val accountNumber: String? = null,
    val cardLastFour: String? = null,
    val remainingBalanceMinorUnits: Long? = null,
    val referenceNumber: String? = null,
    val description: String? = null,
    val originalMessage: String,
    val messageFingerprint: String,
    val timestamp: Long = System.currentTimeMillis()
)
