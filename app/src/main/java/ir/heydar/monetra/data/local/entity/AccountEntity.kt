package ir.heydar.monetra.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "balance")
    val balance: Long,
    @ColumnInfo(name = "initial_balance", defaultValue = "0")
    val initialBalance: Long = 0L,
    @ColumnInfo(name = "currency")
    val currency: String = "USD",
    @ColumnInfo(name = "icon")
    val icon: String = "",
    @ColumnInfo(name = "color")
    val color: String = "#4CAF50",
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "purpose", defaultValue = "")
    val purpose: String = "",
    @ColumnInfo(name = "bank_name", defaultValue = "")
    val bankName: String = "",
    @ColumnInfo(name = "account_number", defaultValue = "")
    val accountNumber: String = "",
    @ColumnInfo(name = "card_number", defaultValue = "")
    val cardNumber: String = "",
    @ColumnInfo(name = "holder_name", defaultValue = "")
    val holderName: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
