package ir.heydar.monetra.domain.usecase

import androidx.room.withTransaction
import ir.heydar.monetra.core.database.MonetraDatabase
import ir.heydar.monetra.core.notification.BankTransactionNotifier
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.entity.PendingStatus
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmBankTransactionUseCase @Inject constructor(
    private val database: MonetraDatabase,
    private val pendingDao: PendingBankTransactionDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val notifier: BankTransactionNotifier
) {
    /**
     * Confirms the pending transaction.
     * Idempotent: checks status first so double-tapping or multiple intents don't create duplicate transactions.
     */
    suspend operator fun invoke(
        pendingId: Long,
        overrideAccountId: Long? = null,
        overrideCategoryId: Long? = null,
        overrideDescription: String? = null
    ): Result<Long> = runCatching {
        val pending = pendingDao.getById(pendingId)
            ?: throw IllegalArgumentException("Pending transaction $pendingId not found")

        if (pending.status == PendingStatus.CONFIRMED.name) {
            // Already confirmed, idempotent return
            return@runCatching pending.id
        }

        // Determine which account to assign to
        val targetAccountId = overrideAccountId ?: run {
            val accounts = accountRepository.getAllAccounts().firstOrNull().orEmpty()
            accounts.firstOrNull { it.isDefault }?.id
                ?: accounts.firstOrNull()?.id
                ?: throw IllegalStateException("No account available to assign transaction to")
        }

        val type = try {
            TransactionType.valueOf(pending.transactionType)
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }

        val transaction = Transaction(
            accountId = targetAccountId,
            counterAccountId = null,
            categoryId = overrideCategoryId,
            amountMinorUnits = pending.amountMinorUnits,
            currencyCode = pending.currencyCode,
            type = type,
            description = overrideDescription ?: pending.description,
            referenceNumber = pending.referenceNumber,
            source = TransactionSource.BANK_SMS,
            bankName = pending.bankName,
            timestamp = pending.transactionTimestamp ?: pending.createdAt
        )

        val txId = database.withTransaction {
            val createdId = transactionRepository.createTransaction(transaction)
            pendingDao.updateStatus(pending.id, PendingStatus.CONFIRMED.name)
            createdId
        }

        notifier.cancelNotification(pending.notificationId)
        txId
    }
}
