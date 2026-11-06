package ir.heydar.monetra.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object MonetraNotificationChannel {
    const val BANK_TRANSACTIONS_ID = "bank_transactions"
    const val BANK_TRANSACTIONS_NAME = "Bank Transactions"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BANK_TRANSACTIONS_ID,
                BANK_TRANSACTIONS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming bank transactions requiring confirmation or review"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
