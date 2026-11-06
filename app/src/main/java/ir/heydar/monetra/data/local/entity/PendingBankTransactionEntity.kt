package ir.heydar.monetra.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Holds a parsed bank transaction that is waiting for the user to confirm or reject it.
 * The [messageFingerprint] unique index prevents the same message from creating two pending rows.
 * [cardLastFour] stores only the last 4 digits — the full card number is never persisted.
 * [status] follows the [PendingStatus] values.
 */
@Entity(
    tableName = "pending_bank_transactions",
    indices = [
        Index(value = ["message_fingerprint"], unique = true),
        Index(value = ["status"]),
        Index(value = ["expires_at"])
    ]
)
data class PendingBankTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,

    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String,

    /** Last 4 digits of the card only — never the full PAN. */
    @ColumnInfo(name = "card_last_four")
    val cardLastFour: String? = null,

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    /** Unique SHA-256 fingerprint of the normalised message text. */
    @ColumnInfo(name = "message_fingerprint")
    val messageFingerprint: String,

    /** Epoch millis derived from the bank message, null when no date was found. */
    @ColumnInfo(name = "transaction_timestamp")
    val transactionTimestamp: Long? = null,

    /** The Android notification ID posted for this pending entry. */
    @ColumnInfo(name = "notification_id")
    val notificationId: Int,

    /** PENDING | CONFIRMED | REJECTED | EXPIRED */
    @ColumnInfo(name = "status")
    val status: String = PendingStatus.PENDING.name,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** created_at + 24 h; the cleanup worker deletes rows past this point. */
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long = createdAt + 24 * 60 * 60 * 1000L
)

enum class PendingStatus { PENDING, CONFIRMED, REJECTED, EXPIRED }
