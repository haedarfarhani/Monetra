package ir.heydar.monetra.core.navigation

import ir.heydar.monetra.domain.model.TransactionType
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object Transactions : Screen

    @Serializable
    data object Accounts : Screen

    @Serializable
    data object Categories : Screen

    @Serializable
    data object Settings : Screen

    /** Pass a positive id to edit an existing transaction, or leave the default for a new one.
     *  [transactionType] pre-selects the form tab for new transactions (ignored when editing). */
    @Serializable
    data class AddTransaction(
        val transactionId: Long = -1L,
        val transactionType: TransactionType = TransactionType.EXPENSE
    ) : Screen

    @Serializable
    data class TransactionDetail(val transactionId: Long) : Screen

    @Serializable
    data object SmsImport : Screen

    @Serializable
    data object Budgets : Screen

    @Serializable
    data object Savings : Screen

    @Serializable
    data object Analytics : Screen

    @Serializable
    data object Debts : Screen

    @Serializable
    data class BankTransactionReview(val pendingId: Long) : Screen
}
