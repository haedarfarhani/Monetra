package ir.heydar.monetra.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.heydar.monetra.core.utils.Constants
import ir.heydar.monetra.data.local.dao.AccountDao
import ir.heydar.monetra.data.local.dao.BankSmsImportDao
import ir.heydar.monetra.data.local.dao.CategoryDao
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.dao.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMonetraDatabase(
        @ApplicationContext context: Context
    ): MonetraDatabase {
        return Room.databaseBuilder(
            context,
            MonetraDatabase::class.java,
            Constants.DATABASE_NAME
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultCategories(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    seedDefaultCategories(db)
                }
            })
            .build()
    }

    private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        try {
            val cursor = db.query("SELECT COUNT(*) FROM categories")
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()

            if (count == 0) {
                val categories = listOf(
                    // Expenses
                    Triple("خرید و مواد غذایی", "EXPENSE", "#10B981"),
                    Triple("رستوران و کافه", "EXPENSE", "#F59E0B"),
                    Triple("حمل و نقل و خودرو", "EXPENSE", "#3B82F6"),
                    Triple("خرید پوشاک و لباس", "EXPENSE", "#EC4899"),
                    Triple("خانه و مسکن", "EXPENSE", "#8B5CF6"),
                    Triple("قبض و خدمات", "EXPENSE", "#6366F1"),
                    Triple("سلامت و درمان", "EXPENSE", "#EF4444"),
                    Triple("آموزش و تحصیل", "EXPENSE", "#14B8A6"),
                    Triple("تفریح و سرگرمی", "EXPENSE", "#A855F7"),
                    Triple("هدیه و کادو", "EXPENSE", "#F43F5E"),
                    Triple("سایر هزینه‌ها", "EXPENSE", "#6B7280"),

                    // Incomes
                    Triple("حقوق و دستمزد", "INCOME", "#16A36A"),
                    Triple("پاداش و عیدی", "INCOME", "#059669"),
                    Triple("سرمایه‌گذاری و سود", "INCOME", "#0D9488"),
                    Triple("فروش و کسب‌وکار", "INCOME", "#0284C7"),
                    Triple("هدیه و واریزی", "INCOME", "#2563EB"),
                    Triple("سایر درآمدها", "INCOME", "#4B5563")
                )

                db.beginTransaction()
                try {
                    for ((name, type, color) in categories) {
                        db.execSQL(
                            "INSERT INTO categories (name, type, icon, color, is_system) VALUES (?, ?, ?, ?, 1)",
                            arrayOf(name, type, "", color)
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        } catch (_: Exception) {
        }
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: MonetraDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: MonetraDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: MonetraDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideBankSmsImportDao(database: MonetraDatabase): BankSmsImportDao {
        return database.bankSmsImportDao()
    }

    @Provides
    @Singleton
    fun providePendingBankTransactionDao(database: MonetraDatabase): PendingBankTransactionDao {
        return database.pendingBankTransactionDao()
    }
}
