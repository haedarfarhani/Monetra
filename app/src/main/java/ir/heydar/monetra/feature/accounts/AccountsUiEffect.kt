package ir.heydar.monetra.feature.accounts

sealed interface AccountsUiEffect {
    data class ShowMessage(val message: String) : AccountsUiEffect
}
