package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.text.TextNormalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves which bank sent a message. Bank senders are numeric short codes, but they differ per
 * operator and change over time, so the message body is used as a fallback signal.
 */
@Singleton
class BankSenderResolver @Inject constructor() {

    private val knownSenders = mapOf(
        "9830001111" to "Melli",
        "9830002222" to "Mellat",
        "9830003333" to "Saderat",
        "9830004444" to "Tejarat"
    )

    private val bodyHints = listOf(
        "بانک ملی" to "Melli",
        "بانک ملت" to "Mellat",
        "بانک صادرات" to "Saderat",
        "بانک تجارت" to "Tejarat"
    )

    fun resolve(sender: String?, normalizedMessage: String): String? {
        val normalizedSender = sender?.let { TextNormalizer.digitsOf(it) }.orEmpty()
        if (normalizedSender.isNotEmpty()) {
            knownSenders[normalizedSender]?.let { return it }
            knownSenders[normalizedSender.takeLast(10)]?.let { return it }
        }
        return bodyHints.firstOrNull { (hint, _) -> normalizedMessage.contains(hint) }?.second
    }
}
