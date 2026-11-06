package ir.heydar.monetra.feature.accounts

import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.Account

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val totalBalance: Long = 0L,
    val defaultCurrency: AppCurrency = AppCurrency.USD,
    val language: String = "en",
    val isSheetVisible: Boolean = false,
    /** Non-null while the editor sheet edits an existing account. */
    val editingAccount: Account? = null,
    /** Non-null while the delete confirmation dialog is shown. */
    val accountToDelete: Account? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)
