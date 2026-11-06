package ir.heydar.monetra.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per imported bank message. The unique fingerprint is the deduplication guarantee: the
 * same message can never create two transactions.
 *
 * [rawMessage] is only kept while the message is pending review and is cleared once it is approved
 * or rejected, so raw bank text does not linger in the database.
 */
@Entity(
    tableName = "bank_sms_import",
    indices = [
        Index(value = ["message_fingerprint"], unique = true),
        Index(value = ["transaction_reference"])
    ]
)
data class BankSmsImportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,
    @ColumnInfo(name = "sender_address")
    val senderAddress: String? = null,
    @ColumnInfo(name = "message_fingerprint")
    val messageFingerprint: String,
    @ColumnInfo(name = "transaction_reference")
    val transactionReference: String? = null,
    @ColumnInfo(name = "parsing_status")
    val parsingStatus: String,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,
    @ColumnInfo(name = "parsing_error")
    val parsingError: String? = null,
    @ColumnInfo(name = "raw_message")
    val rawMessage: String? = null
)
