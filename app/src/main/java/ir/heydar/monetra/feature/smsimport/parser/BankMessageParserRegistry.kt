package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.text.MessageFingerprint
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
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
    private val senderResolver: BankSenderResolver
) {

    fun parse(rawMessage: String, sender: String? = null): BankTransactionDraft {
        val normalized = normalizer.normalize(rawMessage)
        val fingerprint = MessageFingerprint.of(normalized)

        if (normalized.isEmpty() || !classifier.isFinancialTransaction(normalized)) {
            return BankTransactionDraft(
                originalMessage = rawMessage,
                fingerprint = fingerprint,
                parsingStatus = ParsingStatus.FAILED,
                parsingError = "not_financial"
            )
        }

        val resolvedSender = senderResolver.resolve(sender, normalized)
        val ordered = parsers.sortedBy { if (it.isFallback) 1 else 0 }

        ordered.forEach { parser ->
            if (!parser.matches(normalized, resolvedSender)) return@forEach
            val draft = parser.parse(normalized, resolvedSender, rawMessage) ?: return@forEach
            return draft.copy(
                originalMessage = rawMessage,
                fingerprint = fingerprint,
                senderAddress = draft.senderAddress ?: sender
            )
        }

        return BankTransactionDraft(
            originalMessage = rawMessage,
            fingerprint = fingerprint,
            parsingStatus = ParsingStatus.FAILED,
            parsingError = "no_parser"
        )
    }
}
