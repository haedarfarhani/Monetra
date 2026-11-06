package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
import javax.inject.Inject

class TejaratBankParser @Inject constructor() : BankMessageParser {

    override val bankName: String = "Tejarat"

    override fun matches(normalizedMessage: String, sender: String?): Boolean =
        sender == bankName || normalizedMessage.contains("بانک تجارت") || normalizedMessage.contains("تجارت")

    override fun parse(
        normalizedMessage: String,
        sender: String?,
        rawMessage: String
    ): BankTransactionDraft? {
        val amount = ParserSupport.amount(normalizedMessage) ?: return null
        val type = ParserSupport.transactionType(normalizedMessage)
        val timestamp = ParserSupport.timestamp(normalizedMessage)
        val reference = ParserSupport.referenceNumber(normalizedMessage)

        return BankTransactionDraft(
            bankName = bankName,
            senderAddress = sender,
            transactionType = type,
            amountMinorUnits = amount.minorUnits,
            currencyCode = amount.currency.code,
            cardLastFourDigits = ParserSupport.cardLastFour(normalizedMessage),
            accountBalanceMinorUnits = ParserSupport.balance(normalizedMessage)
                ?.let { ParserSupport.toMinorUnits(it, amount.currency) },
            transactionDate = ParserSupport.dateText(normalizedMessage),
            transactionTime = ParserSupport.timeText(normalizedMessage),
            transactionTimestamp = timestamp,
            referenceNumber = reference,
            description = ParserSupport.description(normalizedMessage),
            originalMessage = rawMessage,
            confidence = ParserSupport.confidence(
                bankSpecific = true,
                amount = amount,
                type = type,
                timestamp = timestamp,
                reference = reference
            ),
            parsingStatus = if (type == null) ParsingStatus.NEEDS_REVIEW else ParsingStatus.PARSED
        )
    }
}
