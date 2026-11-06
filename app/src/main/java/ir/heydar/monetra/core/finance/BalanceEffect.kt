package ir.heydar.monetra.core.finance

import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType

/**
 * Pure balance arithmetic, kept out of the repository so transfers and edits can be unit tested.
 * A transfer moves money between two accounts of the same user, so it must never be counted as
 * income or expense.
 */
object BalanceEffect {

    fun deltaFor(transaction: Transaction, accountId: Long): Long = when (transaction.type) {
        TransactionType.INCOME ->
            if (transaction.accountId == accountId) transaction.amountMinorUnits else 0L

        TransactionType.EXPENSE ->
            if (transaction.accountId == accountId) -transaction.amountMinorUnits else 0L

        TransactionType.TRANSFER -> when (accountId) {
            transaction.accountId -> -transaction.amountMinorUnits
            transaction.counterAccountId -> transaction.amountMinorUnits
            else -> 0L
        }
    }

    fun accountsAffectedBy(transaction: Transaction): List<Long> = when (transaction.type) {
        TransactionType.TRANSFER -> listOfNotNull(transaction.accountId, transaction.counterAccountId)
        else -> listOf(transaction.accountId)
    }
}
