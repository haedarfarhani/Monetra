package ir.heydar.monetra.domain.usecase

import ir.heydar.monetra.core.notification.BankTransactionNotifier
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.entity.PendingStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RejectBankTransactionUseCase @Inject constructor(
    private val pendingDao: PendingBankTransactionDao,
    private val notifier: BankTransactionNotifier
) {
    suspend operator fun invoke(pendingId: Long): Result<Unit> = runCatching {
        val pending = pendingDao.getById(pendingId) ?: return@runCatching
        pendingDao.updateStatus(pending.id, PendingStatus.REJECTED.name)
        notifier.cancelNotification(pending.notificationId)
    }
}
