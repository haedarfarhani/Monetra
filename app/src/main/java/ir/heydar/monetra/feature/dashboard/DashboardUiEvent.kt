package ir.heydar.monetra.feature.dashboard

import ir.heydar.monetra.domain.model.TransactionType

sealed interface DashboardUiEvent {
    data object Refresh : DashboardUiEvent
    data class AddTransactionClicked(val type: TransactionType) : DashboardUiEvent
    data object TogglePrivacy : DashboardUiEvent
    data object PermissionsChanged : DashboardUiEvent
    data class TransactionClicked(val transactionId: Long) : DashboardUiEvent
}

