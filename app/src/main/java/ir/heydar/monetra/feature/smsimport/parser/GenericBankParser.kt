package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
import javax.inject.Inject

/**
 * Last resort for unknown banks. It never claims certainty: the draft is always flagged for review
 * and its confidence stays below the auto-save threshold.
 */
class GenericBankParser @Inject constructor() : BankMessageParser {

    override val bankName: String = "Generic"

    override val isFallback: Boolean = true

    override fun matches(normalizedMessage: String, sender: String?): Boolean = true

    override fun parse(
        normalizedMessage: String,
        sender: String?,
        rawMessage: String
    ): BankTransactionDraft? {
        val amount = ParserSupport.amount(normalizedMessage) ?: return null
        val type = ParserSupport.transactionType(normalizedMessage) ?: return null

        return BankTransactionDraft(
            bankName = null,
            senderAddress = sender,
            transactionType = type,
            amountMinorUnits = amount.minorUnits,
            currencyCode = amount.currency.code,
            cardLastFourDigits = ParserSupport.cardLastFour(normalizedMessage),
            transactionDate = ParserSupport.dateText(normalizedMessage),
            transactionTime = ParserSupport.timeText(normalizedMessage),
            transactionTimestamp = ParserSupport.timestamp(normalizedMessage),
            referenceNumber = ParserSupport.referenceNumber(normalizedMessage),
            description = ParserSupport.description(normalizedMessage),
            originalMessage = rawMessage,
            confidence = ParserSupport.confidence(
                bankSpecific = false,
                amount = amount,
                type = type,
                timestamp = null,
                reference = null
            ),
            parsingStatus = ParsingStatus.NEEDS_REVIEW
        )
    }
}
