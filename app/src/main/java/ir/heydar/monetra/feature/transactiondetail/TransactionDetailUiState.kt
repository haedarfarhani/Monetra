package ir.heydar.monetra.feature.transactiondetail

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null,
    val accountName: String? = null,
    val counterAccountName: String? = null,
    val categoryName: String? = null,
    val currency: AppCurrency = AppCurrency.USD,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en",
    val error: String? = null
)

sealed interface TransactionDetailUiEvent {
    data object Delete : TransactionDetailUiEvent
    data object Edit : TransactionDetailUiEvent
}

sealed interface TransactionDetailUiEffect {
    data object Deleted : TransactionDetailUiEffect
    data object NavigateToEdit : TransactionDetailUiEffect
}
