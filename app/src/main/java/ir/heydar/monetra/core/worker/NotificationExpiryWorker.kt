package ir.heydar.monetra.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.heydar.monetra.core.notification.BankTransactionNotifier
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao

@HiltWorker
class NotificationExpiryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingDao: PendingBankTransactionDao,
    private val notifier: BankTransactionNotifier
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val expired = pendingDao.getExpiredPending(now)

        for (item in expired) {
            notifier.cancelNotification(item.notificationId)
        }

        pendingDao.markExpired(now)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "notification_expiry_worker"
    }
}
