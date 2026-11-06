package ir.heydar.monetra.feature.dashboard

sealed interface DashboardUiEvent {
    data object Refresh : DashboardUiEvent
    data object AddTransactionClicked : DashboardUiEvent
    data object TogglePrivacy : DashboardUiEvent
    data class TransactionClicked(val transactionId: Long) : DashboardUiEvent
}

