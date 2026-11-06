package ir.heydar.monetra.feature.transactionform

import ir.heydar.monetra.domain.model.TransactionType

sealed interface TransactionFormUiEvent {
    data class ChangeType(val type: TransactionType) : TransactionFormUiEvent
    data class ChangeAmount(val value: String) : TransactionFormUiEvent
    data class SelectAccount(val accountId: Long) : TransactionFormUiEvent
    data class SelectCounterAccount(val accountId: Long) : TransactionFormUiEvent
    data class SelectCategory(val categoryId: Long?) : TransactionFormUiEvent
    data class ChangeDate(val utcMillis: Long) : TransactionFormUiEvent
    data class ChangeTime(val hour: Int, val minute: Int) : TransactionFormUiEvent
    data class ChangeDescription(val value: String) : TransactionFormUiEvent
    data class CreateCategory(val name: String) : TransactionFormUiEvent
    data class CreateAccount(
        val name: String,
        val initialBalanceInput: String,
        val accountType: String
    ) : TransactionFormUiEvent
    data object Save : TransactionFormUiEvent
    data object Delete : TransactionFormUiEvent
}

sealed interface TransactionFormUiEffect {
    data object Saved : TransactionFormUiEffect
    data object Deleted : TransactionFormUiEffect
    data class Failed(val message: String) : TransactionFormUiEffect
}
