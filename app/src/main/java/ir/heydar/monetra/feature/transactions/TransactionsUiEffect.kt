package ir.heydar.monetra.feature.transactions

sealed interface TransactionsUiEffect {
    data class NavigateToDetail(val id: Long) : TransactionsUiEffect
    data object NavigateToAdd : TransactionsUiEffect
}
