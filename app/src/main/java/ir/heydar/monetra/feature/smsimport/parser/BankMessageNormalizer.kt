package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.text.TextNormalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalization is deliberately separate from parsing: a message is normalized once and the same
 * normalized form is used for classification, deduplication and every parser attempt.
 */
@Singleton
class BankMessageNormalizer @Inject constructor() {
    fun normalize(rawMessage: String): String = TextNormalizer.normalize(rawMessage)
}
