package ir.heydar.monetra.domain.repository

import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.SmsParseResult

/**
 * Universal SMS Transaction Repository interface for Monetra.
 */
interface SmsTransactionRepository {
    fun parseSms(rawMessage: String, sender: String? = null): SmsParseResult
    suspend fun processIncomingSms(rawMessage: String, sender: String? = null): Long
    suspend fun isDuplicate(fingerprint: String): Boolean
}
