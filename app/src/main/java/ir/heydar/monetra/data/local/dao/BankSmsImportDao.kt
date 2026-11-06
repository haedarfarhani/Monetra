package ir.heydar.monetra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.heydar.monetra.data.local.entity.BankSmsImportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankSmsImportDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImportedMessage(message: BankSmsImportEntity): Long

    @Query("SELECT * FROM bank_sms_import WHERE message_fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): BankSmsImportEntity?

    @Query("SELECT * FROM bank_sms_import WHERE bank_name = :bankName AND transaction_reference = :reference LIMIT 1")
    suspend fun findByReference(bankName: String?, reference: String): BankSmsImportEntity?

    @Query("SELECT * FROM bank_sms_import WHERE id = :id")
    suspend fun getById(id: Long): BankSmsImportEntity?

    @Query("SELECT * FROM bank_sms_import ORDER BY imported_at DESC")
    fun observeAll(): Flow<List<BankSmsImportEntity>>

    @Query("UPDATE bank_sms_import SET parsing_status = :status, transaction_id = :transactionId, raw_message = NULL WHERE id = :id")
    suspend fun markProcessed(id: Long, status: String, transactionId: Long?)

    @Query("DELETE FROM bank_sms_import WHERE parsing_status = :status")
    suspend fun deleteByStatus(status: String)

    @Query("DELETE FROM bank_sms_import WHERE id = :id")
    suspend fun deleteById(id: Long)
}
