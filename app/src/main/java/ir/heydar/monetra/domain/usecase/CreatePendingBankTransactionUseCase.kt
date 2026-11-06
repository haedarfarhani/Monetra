package ir.heydar.monetra.domain.usecase

import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.notification.BankTransactionNotifier
import ir.heydar.monetra.core.notification.PrivacyMode
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import ir.heydar.monetra.domain.model.BankTransactionDraft
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CreatePendingBankTransactionUseCase @Inject constructor(
    private val pendingDao: PendingBankTransactionDao,
    private val preferencesManager: PreferencesManager,
    private val notifier: BankTransactionNotifier
) {
    suspend operator fun invoke(draft: BankTransactionDraft): Long {
        if (draft.fingerprint.isBlank()) return -1L
        if (draft.amountMinorUnits == null || draft.amountMinorUnits <= 0L) return -1L

        // Check if message already exists
        val existing = pendingDao.findByFingerprint(draft.fingerprint)
        if (existing != null) {
            return existing.id
        }

        val notifId = Random.nextInt(1000, 999999)
        val entity = PendingBankTransactionEntity(
            bankName = draft.bankName,
            amountMinorUnits = draft.amountMinorUnits,
            currencyCode = draft.currencyCode ?: "IRR",
            transactionType = draft.transactionType?.name ?: "EXPENSE",
            cardLastFour = draft.cardLastFourDigits,
            referenceNumber = draft.referenceNumber,
            description = draft.description,
            messageFingerprint = draft.fingerprint,
            transactionTimestamp = draft.transactionTimestamp,
            notificationId = notifId
        )

        val insertedId = pendingDao.insert(entity)
        if (insertedId <= 0L) {
            return -1L
        }

        // Post notification if notifications enabled
        val notifEnabled = preferencesManager.bankNotificationsEnabled.first()
        if (notifEnabled) {
            val hideAmounts = !preferencesManager.bankNotificationsShowAmount.first()
            val hideLockscreen = preferencesManager.bankNotificationsHideLockscreen.first()
            val privacyMode = PrivacyMode(
                hideAmounts = hideAmounts,
                hideOnLockscreen = hideLockscreen
            )
            notifier.postReviewNotification(
                pending = entity.copy(id = insertedId),
                privacyMode = privacyMode
            )
        }

        return insertedId
    }
}
