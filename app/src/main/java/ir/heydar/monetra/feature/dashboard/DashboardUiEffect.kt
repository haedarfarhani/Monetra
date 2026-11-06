package ir.heydar.monetra.feature.dashboard

/**
 * One-time side effects for the Dashboard screen (e.g., navigation, showing toast/snackbar).
 * Exposed via Channel -> ReceiveAsFlow in ViewModel to guarantee single handling across lifecycle events.
 */
sealed interface DashboardUiEffect {
    data object NavigateToAddTransaction : DashboardUiEffect
    data class NavigateToTransactionDetail(val transactionId: Long) : DashboardUiEffect
    data class ShowSnackbar(val message: String) : DashboardUiEffect
}

