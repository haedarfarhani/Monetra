package ir.heydar.monetra.feature.accounts

import ir.heydar.monetra.core.currency.AppCurrency

sealed interface AccountsUiEvent {
    data object Refresh : AccountsUiEvent
    data object AddAccountClicked : AccountsUiEvent
    data object DismissSheet : AccountsUiEvent

    /** Opens the editor sheet for an existing account. */
    data class EditAccountClicked(val id: Long) : AccountsUiEvent

    /** [role] is one of ROLE_NONE / ROLE_DEFAULT / ROLE_DAILY / ROLE_SALARY. */
    data class SubmitAccount(
        val name: String,
        val initialBalanceInput: String,
        val currency: AppCurrency,
        val type: String,
        val role: String,
        val bankName: String = "",
        val accountNumber: String = "",
        val cardNumber: String = "",
        val holderName: String = ""
    ) : AccountsUiEvent

    data class DeleteRequested(val id: Long) : AccountsUiEvent
    data object DeleteConfirmed : AccountsUiEvent
    data object DismissDeleteConfirm : AccountsUiEvent
}
