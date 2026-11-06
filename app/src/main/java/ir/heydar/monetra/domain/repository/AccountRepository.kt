package ir.heydar.monetra.domain.repository

import ir.heydar.monetra.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(id: Long): Flow<Account?>
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    fun getTotalBalance(): Flow<Long>
}

