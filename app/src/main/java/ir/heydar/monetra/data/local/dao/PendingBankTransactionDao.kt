package ir.heydar.monetra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingBankTransactionDao {

    /** Insert a new pending row. Returns the new row id, or -1 if the fingerprint already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingBankTransactionEntity): Long

    @Query("SELECT * FROM pending_bank_transactions WHERE message_fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): PendingBankTransactionEntity?

    @Query("SELECT * FROM pending_bank_transactions WHERE id = :id")
    suspend fun getById(id: Long): PendingBankTransactionEntity?

    @Query("SELECT * FROM pending_bank_transactions WHERE status = 'PENDING' ORDER BY created_at DESC")
    fun observePending(): Flow<List<PendingBankTransactionEntity>>

    @Query("UPDATE pending_bank_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM pending_bank_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Removes all rows whose expiry timestamp is before [beforeMillis]. */
    @Query("SELECT * FROM pending_bank_transactions WHERE expires_at < :beforeMillis AND status = 'PENDING'")
    suspend fun getExpiredPending(beforeMillis: Long): List<PendingBankTransactionEntity>

    @Query("UPDATE pending_bank_transactions SET status = 'EXPIRED' WHERE expires_at < :beforeMillis AND status = 'PENDING'")
    suspend fun markExpired(beforeMillis: Long)
}
