package ir.heydar.monetra.domain.repository

import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionFilter
import ir.heydar.monetra.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun getTransactionById(id: Long): Flow<Transaction?>

    /** Creates the transaction and applies its balance effect atomically. */
    suspend fun createTransaction(transaction: Transaction): Long

    /** Replaces an existing transaction, reversing the old balance effect first. */
    suspend fun updateTransaction(transaction: Transaction)

    /** Deletes a transaction and reverses its balance effect. */
    suspend fun deleteTransaction(id: Long)

    suspend fun searchTransactions(filter: TransactionFilter): List<Transaction>

    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Long>
    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Long>
}
