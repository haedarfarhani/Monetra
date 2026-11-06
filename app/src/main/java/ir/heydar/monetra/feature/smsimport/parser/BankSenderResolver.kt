package ir.heydar.monetra.feature.smsimport.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves which bank sent a message using [BankDetector].
 */
@Singleton
class BankSenderResolver @Inject constructor(
    private val bankDetector: BankDetector
) {
    fun resolve(sender: String?, normalizedMessage: String): String? {
        return bankDetector.detect(sender, normalizedMessage).bankInfo?.nameEn
    }
}
