package ir.heydar.monetra.domain.repository

import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.Transaction

sealed interface SaveImportResult {
    data class Saved(val transactionId: Long) : SaveImportResult
    data object Duplicate : SaveImportResult
}

interface SmsImportRepository {
    /** True when this message (or its reference number) was already imported. */
    suspend fun isDuplicate(draft: BankTransactionDraft): Boolean

    /** Stores the import row and creates the transaction; never saves twice. */
    suspend fun saveDraft(draft: BankTransactionDraft, transaction: Transaction): SaveImportResult

    /** Records that the user discarded this message and drops its raw text. */
    suspend fun rejectDraft(draft: BankTransactionDraft)

    /** Removes drafts that are no longer needed, keeping processed history lean. */
    suspend fun clearPending()
}
