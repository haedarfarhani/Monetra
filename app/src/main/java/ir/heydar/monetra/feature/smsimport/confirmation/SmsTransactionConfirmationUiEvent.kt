package ir.heydar.monetra.feature.smsimport.confirmation

import ir.heydar.monetra.domain.model.TransactionType

sealed interface SmsTransactionConfirmationUiEvent {
    data class SelectAccount(val accountId: Long) : SmsTransactionConfirmationUiEvent
    data class SelectCategory(val categoryId: Long?) : SmsTransactionConfirmationUiEvent
    data class ChangeDescription(val value: String) : SmsTransactionConfirmationUiEvent
    data class ChangeType(val type: TransactionType) : SmsTransactionConfirmationUiEvent
    data object Confirm : SmsTransactionConfirmationUiEvent
    data object Reject : SmsTransactionConfirmationUiEvent
}
