package ir.heydar.monetra.data.repository

import ir.heydar.monetra.data.local.dao.BankSmsImportDao
import ir.heydar.monetra.data.mapper.toImportEntity
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.repository.SaveImportResult
import ir.heydar.monetra.domain.repository.SmsImportRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsImportRepositoryImpl @Inject constructor(
    private val importDao: BankSmsImportDao,
    private val transactionRepository: TransactionRepository
) : SmsImportRepository {

    override suspend fun isDuplicate(draft: BankTransactionDraft): Boolean {
        if (draft.fingerprint.isBlank()) return false
        if (importDao.findByFingerprint(draft.fingerprint) != null) return true
        val reference = draft.referenceNumber ?: return false
        return importDao.findByReference(draft.bankName, reference) != null
    }

    override suspend fun saveDraft(
        draft: BankTransactionDraft,
        transaction: Transaction
    ): SaveImportResult {
        if (isDuplicate(draft)) return SaveImportResult.Duplicate

        val importId = importDao.insertImportedMessage(draft.toImportEntity(ParsingStatus.PARSED))
        if (importId <= 0L) return SaveImportResult.Duplicate

        val transactionId = transactionRepository.createTransaction(transaction)
        importDao.markProcessed(importId, ParsingStatus.SAVED.name, transactionId)
        return SaveImportResult.Saved(transactionId)
    }

    override suspend fun rejectDraft(draft: BankTransactionDraft) {
        if (draft.fingerprint.isBlank()) return
        val importId = importDao.insertImportedMessage(draft.toImportEntity(ParsingStatus.REJECTED))
        if (importId > 0L) {
            importDao.markProcessed(importId, ParsingStatus.REJECTED.name, null)
        }
    }

    override suspend fun clearPending() {
        importDao.deleteByStatus(ParsingStatus.NEEDS_REVIEW.name)
        importDao.deleteByStatus(ParsingStatus.PARSED.name)
        importDao.deleteByStatus(ParsingStatus.FAILED.name)
    }
}
