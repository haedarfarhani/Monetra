package ir.heydar.monetra.feature.bankreview

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType

enum class ReviewScreenStatus {
    LOADING,
    READY,
    CONFIRMED,
    REJECTED,
    EXPIRED,
    ERROR
}

data class BankTransactionReviewUiState(
    val pending: PendingBankTransactionEntity? = null,
    val status: ReviewScreenStatus = ReviewScreenStatus.LOADING,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val currency: AppCurrency = AppCurrency.IRR,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
) {
    val categoriesForType: List<Category> get() = categories.filter { it.type == type }
}

sealed interface BankTransactionReviewUiEvent {
    data class SelectAccount(val accountId: Long) : BankTransactionReviewUiEvent
    data class SelectCategory(val categoryId: Long?) : BankTransactionReviewUiEvent
    data class ChangeDescription(val value: String) : BankTransactionReviewUiEvent
    data class ChangeType(val type: TransactionType) : BankTransactionReviewUiEvent
    data object Confirm : BankTransactionReviewUiEvent
    data object Reject : BankTransactionReviewUiEvent
}
