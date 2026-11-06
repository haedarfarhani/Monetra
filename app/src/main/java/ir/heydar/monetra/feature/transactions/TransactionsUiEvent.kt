package ir.heydar.monetra.feature.transactions

import ir.heydar.monetra.domain.model.TransactionType

sealed interface TransactionsUiEvent {
    data object Refresh : TransactionsUiEvent
    data class SearchChanged(val query: String) : TransactionsUiEvent
    data class TypeFilterChanged(val type: TransactionType?) : TransactionsUiEvent
    data class AccountFilterChanged(val accountId: Long?) : TransactionsUiEvent
    data class CategoryFilterChanged(val categoryId: Long?) : TransactionsUiEvent
    data class DateRangeChanged(val startMillis: Long?, val endMillis: Long?) : TransactionsUiEvent
    data object ClearFilters : TransactionsUiEvent
    data object LoadMore : TransactionsUiEvent
    data class TransactionClicked(val id: Long) : TransactionsUiEvent
    data object AddClicked : TransactionsUiEvent


    /** Deletes the transaction and reverses its balance effect. */
    data class DeleteTransaction(val id: Long) : TransactionsUiEvent
}
