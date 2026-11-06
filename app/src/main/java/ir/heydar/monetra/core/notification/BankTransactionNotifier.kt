package ir.heydar.monetra.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.heydar.monetra.MainActivity
import ir.heydar.monetra.R
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankTransactionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun postReviewNotification(
        pending: PendingBankTransactionEntity,
        privacyMode: PrivacyMode
    ) {
        val title = pending.bankName?.let {
            context.getString(R.string.notif_bank_title_with_bank, it)
        } ?: context.getString(R.string.notif_bank_title)

        val amountDisplay = if (privacyMode.hideAmounts) {
            "•••"
        } else {
            val currency = AppCurrency.fromCode(pending.currencyCode)
            MoneyFormatter.format(pending.amountMinorUnits, currency)
        }

        val typeLabel = when (pending.transactionType) {
            "INCOME" -> context.getString(R.string.transaction_type_income)
            "TRANSFER" -> context.getString(R.string.transaction_type_transfer)
            else -> context.getString(R.string.transaction_type_expense)
        }

        val contentText = "$typeLabel: $amountDisplay"

        // Review Action (opens app directly to BankTransactionReview)
        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PENDING_TRANSACTION_ID, pending.id)
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            pending.notificationId * 10 + 1,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Confirm Action Intent
        val confirmIntent = Intent(context, BankTransactionActionReceiver::class.java).apply {
            action = ACTION_CONFIRM
            putExtra(EXTRA_PENDING_TRANSACTION_ID, pending.id)
            putExtra(EXTRA_NOTIFICATION_ID, pending.notificationId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            pending.notificationId * 10 + 2,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject Action Intent
        val rejectIntent = Intent(context, BankTransactionActionReceiver::class.java).apply {
            action = ACTION_REJECT
            putExtra(EXTRA_PENDING_TRANSACTION_ID, pending.id)
            putExtra(EXTRA_NOTIFICATION_ID, pending.notificationId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            pending.notificationId * 10 + 3,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MonetraNotificationChannel.BANK_TRANSACTIONS_ID)
            .setSmallIcon(R.drawable.ic_notification_bank)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                pending.description?.let { "$contentText\n$it" } ?: contentText
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(reviewPendingIntent)
            .setAutoCancel(true)
            .setVisibility(
                if (privacyMode.hideOnLockscreen) {
                    NotificationCompat.VISIBILITY_PRIVATE
                } else {
                    NotificationCompat.VISIBILITY_PUBLIC
                }
            )
            .addAction(
                0,
                context.getString(R.string.notif_bank_confirm),
                confirmPendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.notif_bank_reject),
                rejectPendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.notif_bank_review),
                reviewPendingIntent
            )

        notificationManager.notify(pending.notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val ACTION_CONFIRM = "ir.heydar.monetra.action.CONFIRM_BANK_TRANSACTION"
        const val ACTION_REJECT = "ir.heydar.monetra.action.REJECT_BANK_TRANSACTION"
        const val EXTRA_PENDING_TRANSACTION_ID = "extra_pending_transaction_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
