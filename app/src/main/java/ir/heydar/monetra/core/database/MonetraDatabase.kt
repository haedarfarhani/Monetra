package ir.heydar.monetra.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.heydar.monetra.data.local.converter.Converters
import ir.heydar.monetra.data.local.dao.AccountDao
import ir.heydar.monetra.data.local.dao.BankSmsImportDao
import ir.heydar.monetra.data.local.dao.CategoryDao
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.dao.TransactionDao
import ir.heydar.monetra.data.local.entity.AccountEntity
import ir.heydar.monetra.data.local.entity.BankSmsImportEntity
import ir.heydar.monetra.data.local.entity.CategoryEntity
import ir.heydar.monetra.data.local.entity.PendingBankTransactionEntity
import ir.heydar.monetra.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BankSmsImportEntity::class,
        PendingBankTransactionEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MonetraDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankSmsImportDao(): BankSmsImportDao
    abstract fun pendingBankTransactionDao(): PendingBankTransactionDao
}

/**
 * Adds the columns needed for manual transactions, transfers, bank SMS imports and per-account
 * initial balances. Existing rows are preserved: the old `amount` is converted to minor units
 * using the owning account's currency, and `initial_balance` is backfilled so that
 * `initial_balance + ledger` keeps the balance the user already sees.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions_v2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `account_id` INTEGER NOT NULL,
                `counter_account_id` INTEGER,
                `category_id` INTEGER,
                `amount_minor_units` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `description` TEXT,
                `notes` TEXT,
                `reference_number` TEXT,
                `source` TEXT NOT NULL,
                `bank_name` TEXT,
                `transfer_group_id` TEXT,
                `timestamp` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`counter_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL ,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `transactions_v2` (
                `id`, `account_id`, `counter_account_id`, `category_id`, `amount_minor_units`,
                `currency_code`, `type`, `description`, `notes`, `reference_number`, `source`,
                `bank_name`, `transfer_group_id`, `timestamp`, `created_at`, `updated_at`
            )
            SELECT
                t.`id`,
                t.`account_id`,
                NULL,
                t.`category_id`,
                CASE WHEN COALESCE(a.`currency`, 'USD') = 'USD' THEN t.`amount` * 100 ELSE t.`amount` END,
                COALESCE(a.`currency`, 'USD'),
                t.`type`,
                t.`note`,
                NULL,
                NULL,
                'MANUAL',
                NULL,
                NULL,
                t.`timestamp`,
                t.`created_at`,
                t.`created_at`
            FROM `transactions` t
            LEFT JOIN `accounts` a ON a.`id` = t.`account_id`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_v2` RENAME TO `transactions`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_counter_account_id` ON `transactions` (`counter_account_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transfer_group_id` ON `transactions` (`transfer_group_id`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bank_sms_import` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bank_name` TEXT,
                `sender_address` TEXT,
                `message_fingerprint` TEXT NOT NULL,
                `transaction_reference` TEXT,
                `parsing_status` TEXT NOT NULL,
                `imported_at` INTEGER NOT NULL,
                `transaction_id` INTEGER,
                `parsing_error` TEXT,
                `raw_message` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bank_sms_import_message_fingerprint` ON `bank_sms_import` (`message_fingerprint`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bank_sms_import_transaction_reference` ON `bank_sms_import` (`transaction_reference`)")

        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `initial_balance` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE `accounts` SET `initial_balance` = `balance` - COALESCE((
                SELECT SUM(CASE
                    WHEN t.`type` = 'INCOME' AND t.`account_id` = `accounts`.`id` THEN t.`amount_minor_units`
                    WHEN t.`type` = 'EXPENSE' AND t.`account_id` = `accounts`.`id` THEN -t.`amount_minor_units`
                    WHEN t.`type` = 'TRANSFER' AND t.`account_id` = `accounts`.`id` THEN -t.`amount_minor_units`
                    WHEN t.`type` = 'TRANSFER' AND t.`counter_account_id` = `accounts`.`id` THEN t.`amount_minor_units`
                    ELSE 0 END)
                FROM `transactions` t
                WHERE t.`account_id` = `accounts`.`id` OR t.`counter_account_id` = `accounts`.`id`
            ), 0)
            """.trimIndent()
        )
    }
}

/** Adds the pending_bank_transactions table introduced in version 3. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_bank_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bank_name` TEXT,
                `amount_minor_units` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL,
                `transaction_type` TEXT NOT NULL,
                `card_last_four` TEXT,
                `reference_number` TEXT,
                `description` TEXT,
                `message_fingerprint` TEXT NOT NULL,
                `transaction_timestamp` INTEGER,
                `notification_id` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `created_at` INTEGER NOT NULL,
                `expires_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_bank_transactions_message_fingerprint` " +
                "ON `pending_bank_transactions` (`message_fingerprint`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_bank_transactions_status` " +
                "ON `pending_bank_transactions` (`status`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_bank_transactions_expires_at` " +
                "ON `pending_bank_transactions` (`expires_at`)"
        )
    }
}

/**
 * Adds the accounts.purpose column introduced in version 4, used to tag an account as
 * the daily-spending or salary account. Existing rows keep their data ("no role").
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN purpose TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Adds the bank-account detail columns introduced in version 5 (bank name, account
 * number, card number and holder name). Existing rows keep their data (empty values).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN bank_name TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE accounts ADD COLUMN account_number TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE accounts ADD COLUMN card_number TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE accounts ADD COLUMN holder_name TEXT NOT NULL DEFAULT ''")
    }
}
