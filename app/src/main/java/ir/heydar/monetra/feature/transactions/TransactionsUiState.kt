package ir.heydar.monetra.feature.transactions

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType

data class TransactionGroup(
    val dateLabel: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long,
    val transactions: List<Transaction>
)

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val groups: List<TransactionGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val search: String = "",
    val typeFilter: TransactionType? = null,
    val accountFilter: Long? = null,
    val categoryFilter: Long? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val currency: AppCurrency = AppCurrency.USD,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en",
    val error: String? = null
) {
    val hasActiveFilters: Boolean
        get() = typeFilter != null || accountFilter != null || categoryFilter != null ||
            startMillis != null || endMillis != null || search.isNotBlank()
}
