package ir.heydar.monetra.feature.transactionform

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType

data class TransactionFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val currency: AppCurrency = AppCurrency.USD,
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val categoryId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val amountError: Boolean = false,
    val accountError: Boolean = false,
    val counterAccountError: Boolean = false,
    val categoryError: Boolean = false,
    val isSaving: Boolean = false,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en"
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    val categoriesForType: List<Category>
        get() = categories.filter { it.type == type }
}
