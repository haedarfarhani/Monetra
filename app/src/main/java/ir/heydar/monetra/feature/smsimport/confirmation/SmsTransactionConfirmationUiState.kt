package ir.heydar.monetra.feature.smsimport.confirmation

import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType

enum class ConfirmationStatus {
    LOADING,
    READY,
    CONFIRMED,
    REJECTED,
    EXPIRED,
    ERROR
}

data class SmsTransactionConfirmationUiState(
    val status: ConfirmationStatus = ConfirmationStatus.LOADING,
    val pending: PendingBankTransactionEntity? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
