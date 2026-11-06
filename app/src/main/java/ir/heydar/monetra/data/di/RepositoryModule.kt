package ir.heydar.monetra.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.heydar.monetra.core.common.DefaultDispatcherProvider
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.data.repository.AccountRepositoryImpl
import ir.heydar.monetra.data.repository.CategoryRepositoryImpl
import ir.heydar.monetra.data.repository.SmsImportRepositoryImpl
import ir.heydar.monetra.data.repository.SmsTransactionRepositoryImpl
import ir.heydar.monetra.data.repository.TransactionRepositoryImpl
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.SmsImportRepository
import ir.heydar.monetra.domain.repository.SmsTransactionRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSmsImportRepository(
        impl: SmsImportRepositoryImpl
    ): SmsImportRepository

    @Binds
    @Singleton
    abstract fun bindSmsTransactionRepository(
        impl: SmsTransactionRepositoryImpl
    ): SmsTransactionRepository

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider
    ): DispatcherProvider
}
