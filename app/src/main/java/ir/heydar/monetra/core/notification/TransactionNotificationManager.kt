package ir.heydar.monetra.core.notification

import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionNotificationManager @Inject constructor(
    private val notifier: BankTransactionNotifier
) {
    fun postTransactionNotification(
        pending: PendingBankTransactionEntity,
        privacyMode: PrivacyMode
    ) {
        notifier.postReviewNotification(pending, privacyMode)
    }

    fun cancelNotification(notificationId: Int) {
        notifier.cancelNotification(notificationId)
    }
}
