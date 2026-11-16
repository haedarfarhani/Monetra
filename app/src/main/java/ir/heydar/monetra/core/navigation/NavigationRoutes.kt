package ir.heydar.monetra.core.navigation

import androidx.annotation.Keep
import ir.heydar.monetra.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Keep
sealed interface Screen {
    @Serializable
    @Keep
    data object Onboarding : Screen

    @Serializable
    @Keep
    data object Dashboard : Screen

    @Serializable
    @Keep
    data object Transactions : Screen

    @Serializable
    @Keep
    data object Accounts : Screen

    @Serializable
    @Keep
    data object Categories : Screen

    @Serializable
    @Keep
    data object Settings : Screen

    /** Pass a positive id to edit an existing transaction, or leave the default for a new one.
     *  [transactionType] pre-selects the form tab for new transactions (ignored when editing). */
    @Serializable
    @Keep
    data class AddTransaction(
        val transactionId: Long = -1L,
        val transactionType: TransactionType = TransactionType.EXPENSE
    ) : Screen

    @Serializable
    @Keep
    data class TransactionDetail(val transactionId: Long) : Screen

    @Serializable
    @Keep
    data object SmsImport : Screen

    @Serializable
    @Keep
    data object Budgets : Screen

    @Serializable
    @Keep
    data object Savings : Screen

    @Serializable
    @Keep
    data object Analytics : Screen

    @Serializable
    @Keep
    data object Debts : Screen

    @Serializable
    @Keep
    data class BankTransactionReview(val pendingId: Long) : Screen
}

