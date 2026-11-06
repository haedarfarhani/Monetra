package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
import javax.inject.Inject

class ParsianBankParser @Inject constructor() : BankMessageParser {

    override val bankName: String = "Parsian"

    override fun matches(normalizedMessage: String, sender: String?): Boolean =
        sender == bankName || normalizedMessage.contains("بانک پارسیان") || normalizedMessage.contains("پارسیان")

    override fun parse(
        normalizedMessage: String,
        sender: String?,
        rawMessage: String
    ): BankTransactionDraft? {
        val amount = ParserSupport.amount(normalizedMessage) ?: return null
        val type = ParserSupport.transactionType(normalizedMessage)
        val bankType = ParserSupport.bankTransactionType(normalizedMessage)
        val timestamp = ParserSupport.timestamp(normalizedMessage)
        val reference = ParserSupport.referenceNumber(normalizedMessage)
        val account = ParserSupport.accountNumber(normalizedMessage)
        val card = ParserSupport.cardLastFour(normalizedMessage)
        val balance = ParserSupport.balance(normalizedMessage)

        return BankTransactionDraft(
            bankName = bankName,
            senderAddress = sender,
            transactionType = type,
            bankTransactionType = bankType,
            amountMinorUnits = amount.minorUnits,
            currencyCode = amount.currency.code,
            accountNumber = account,
            cardLastFourDigits = card,
            accountBalanceMinorUnits = balance?.let { ParserSupport.toMinorUnits(it, amount.currency) },
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
