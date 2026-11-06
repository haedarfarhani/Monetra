package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.text.MessageFingerprint
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
import ir.heydar.monetra.domain.model.SmsParseResult
import ir.heydar.monetra.domain.model.SmsProcessingStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the bank specific parsers first and only then the generic fallback, so a message is never
 * interpreted by the weakest parser when a better one understands it.
 */
@Singleton
class BankMessageParserRegistry @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards BankMessageParser>,
    private val normalizer: BankMessageNormalizer,
    private val classifier: TransactionMessageClassifier,
    private val senderResolver: BankSenderResolver,
    private val bankDetector: BankDetector
) {

    fun parse(rawMessage: String, sender: String? = null): BankTransactionDraft {
        val result = parseSmsResult(rawMessage, sender)
        return result.draft ?: BankTransactionDraft(
            originalMessage = rawMessage,
            fingerprint = MessageFingerprint.of(normalizer.normalize(rawMessage)),
            parsingStatus = ParsingStatus.FAILED,
            parsingError = result.validationErrors.firstOrNull() ?: "failed"
        )
    }

    fun parseSmsResult(rawMessage: String, sender: String? = null): SmsParseResult {
        val normalized = normalizer.normalize(rawMessage)
        val fingerprint = MessageFingerprint.of(normalized)

        if (normalized.isEmpty()) {
            return SmsParseResult(
                status = SmsProcessingStatus.INVALID_TRANSACTION,
                draft = null,
                bankInfo = null,
                confidence = 0,
                validationErrors = listOf("empty_message")
            )
        }

        if (!classifier.isFinancialTransaction(normalized)) {
            return SmsParseResult(
                status = SmsProcessingStatus.INVALID_TRANSACTION,
                draft = null,
                bankInfo = null,
                confidence = 0,
                validationErrors = listOf("not_financial")
            )
        }

        val detection = bankDetector.detect(sender, normalized)
        val resolvedSender = detection.bankInfo?.nameEn ?: senderResolver.resolve(sender, normalized)
        val ordered = parsers.sortedBy { if (it.isFallback) 1 else 0 }

        for (parser in ordered) {
            if (!parser.matches(normalized, resolvedSender)) continue
            val draft = parser.parse(normalized, resolvedSender, rawMessage) ?: continue

            val finalDraft = draft.copy(
                originalMessage = rawMessage,
                fingerprint = fingerprint,
                bankName = draft.bankName ?: detection.bankInfo?.nameEn,
                senderAddress = draft.senderAddress ?: sender
            )

            val status = if (parser.isFallback) SmsProcessingStatus.PARTIAL else SmsProcessingStatus.SUCCESS

            return SmsParseResult(
                status = status,
                draft = finalDraft,
                bankInfo = detection.bankInfo,
                confidence = finalDraft.confidence
            )
        }

        return SmsParseResult(
            status = SmsProcessingStatus.UNKNOWN_FORMAT,
            draft = null,
            bankInfo = detection.bankInfo,
            confidence = 0,
            validationErrors = listOf("no_matching_parser")
        )
    }
}
