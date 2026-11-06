package ir.heydar.monetra.data.repository

import androidx.room.withTransaction
import ir.heydar.monetra.core.common.AppException
import ir.heydar.monetra.core.database.MonetraDatabase
import ir.heydar.monetra.core.finance.BalanceEffect
import ir.heydar.monetra.data.local.dao.AccountDao
import ir.heydar.monetra.data.local.dao.TransactionDao
import ir.heydar.monetra.data.mapper.toDomain
import ir.heydar.monetra.data.mapper.toEntity
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionFilter
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val database: MonetraDatabase
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type.name).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionById(id: Long): Flow<Transaction?> =
        transactionDao.getTransactionById(id).map { it?.toDomain() }

    override suspend fun createTransaction(transaction: Transaction): Long {
        validate(transaction)
        return database.withTransaction {
            applyBalance(transaction, sign = 1L)
            transactionDao.insertTransaction(transaction.toEntity())
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        validate(transaction)
        database.withTransaction {
            val existing = transactionDao.getTransactionByIdOnce(transaction.id)
                ?: throw AppException.NotFoundException("Transaction ${transaction.id} not found")
            applyBalance(existing.toDomain(), sign = -1L)
            val updated = transaction.copy(
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            applyBalance(updated, sign = 1L)
            transactionDao.updateTransaction(updated.toEntity())
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        database.withTransaction {
            val existing = transactionDao.getTransactionByIdOnce(id) ?: return@withTransaction
            applyBalance(existing.toDomain(), sign = -1L)
            transactionDao.deleteTransactionById(id)
        }
    }

    override suspend fun searchTransactions(filter: TransactionFilter): List<Transaction> =
        transactionDao.searchTransactions(
            search = filter.query.trim(),
            type = filter.type?.name,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            startMillis = filter.startMillis,
            endMillis = filter.endMillis,
            limit = filter.limit,
            offset = filter.offset
        ).map { it.toDomain() }

    override fun getTotalIncome(startDate: Long, endDate: Long): Flow<Long> =
        transactionDao.getTotalAmountByTypeAndDateRange(
            type = TransactionType.INCOME.name,
            startDate = startDate,
            endDate = endDate
        ).map { it ?: 0L }

    override fun getTotalExpense(startDate: Long, endDate: Long): Flow<Long> =
        transactionDao.getTotalAmountByTypeAndDateRange(
            type = TransactionType.EXPENSE.name,
            startDate = startDate,
            endDate = endDate
        ).map { it ?: 0L }

    private suspend fun applyBalance(transaction: Transaction, sign: Long) {
        val now = System.currentTimeMillis()
        BalanceEffect.accountsAffectedBy(transaction).forEach { accountId ->
            val delta = BalanceEffect.deltaFor(transaction, accountId) * sign
            if (delta != 0L) {
                accountDao.adjustBalance(accountId, delta, now)
            }
        }
    }

    private fun validate(transaction: Transaction) {
        if (transaction.amountMinorUnits <= 0L) {
            throw AppException.ValidationException("Amount must be greater than zero")
        }
        if (transaction.type == TransactionType.TRANSFER) {
            val target = transaction.counterAccountId
                ?: throw AppException.ValidationException("A transfer needs a destination account")
            if (target == transaction.accountId) {
                throw AppException.ValidationException("Source and destination accounts must differ")
            }
        }
    }
}
