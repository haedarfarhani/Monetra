package ir.heydar.monetra.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import ir.heydar.monetra.domain.usecase.ConfirmBankTransactionUseCase
import ir.heydar.monetra.domain.usecase.RejectBankTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankTransactionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var confirmUseCase: ConfirmBankTransactionUseCase

    @Inject
    lateinit var rejectUseCase: RejectBankTransactionUseCase

    @Inject
    lateinit var notifier: BankTransactionNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val pendingId = intent.getLongExtra(BankTransactionNotifier.EXTRA_PENDING_TRANSACTION_ID, -1L)
        val notifId = intent.getIntExtra(BankTransactionNotifier.EXTRA_NOTIFICATION_ID, -1)

        if (pendingId <= 0L) {
            if (notifId > 0) notifier.cancelNotification(notifId)
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                when (intent.action) {
                    BankTransactionNotifier.ACTION_CONFIRM -> {
                        confirmUseCase(pendingId)
                    }
                    BankTransactionNotifier.ACTION_REJECT -> {
                        rejectUseCase(pendingId)
                    }
                }
            } finally {
                if (notifId > 0) {
                    notifier.cancelNotification(notifId)
                }
                pendingResult.finish()
            }
        }
    }
}
