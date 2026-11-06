package ir.heydar.monetra.feature.smsimport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import ir.heydar.monetra.domain.repository.SmsTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsTransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }

        if (body.isBlank()) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                smsRepository.processIncomingSms(body, sender)
            } catch (_: Exception) {
                // Log or handle errors gracefully without crashing the receiver
            } finally {
                pendingResult.finish()
            }
        }
    }
}
