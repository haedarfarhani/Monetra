package ir.heydar.monetra.domain.model

/**
 * Full result of attempting to parse an SMS message as a bank transaction.
 */
data class SmsParseResult(
    val status: SmsProcessingStatus,
    val draft: BankTransactionDraft?,
    val bankInfo: BankInfo?,
    val confidence: Int = 0,
    val validationErrors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val isSuccessful: Boolean get() = status == SmsProcessingStatus.SUCCESS || status == SmsProcessingStatus.PARTIAL
}
