package ir.heydar.monetra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.heydar.monetra.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(amount_minor_units) FROM transactions WHERE type = :type AND timestamp BETWEEN :startDate AND :endDate")
    fun getTotalAmountByTypeAndDateRange(type: String, startDate: Long, endDate: Long): Flow<Long?>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:search = '' OR description LIKE '%' || :search || '%'
            OR notes LIKE '%' || :search || '%'
            OR reference_number LIKE '%' || :search || '%')
          AND (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR account_id = :accountId OR counter_account_id = :accountId)
          AND (:categoryId IS NULL OR category_id = :categoryId)
          AND timestamp BETWEEN :startMillis AND :endMillis
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchTransactions(
        search: String,
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        startMillis: Long,
        endMillis: Long,
        limit: Int,
        offset: Int
    ): List<TransactionEntity>
}
