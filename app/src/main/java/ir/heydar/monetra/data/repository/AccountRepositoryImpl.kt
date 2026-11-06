package ir.heydar.monetra.data.repository

import ir.heydar.monetra.data.local.dao.AccountDao
import ir.heydar.monetra.data.mapper.toDomain
import ir.heydar.monetra.data.mapper.toEntity
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountById(id: Long): Flow<Account?> {
        return accountDao.getAccountById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override fun getTotalBalance(): Flow<Long> {
        return accountDao.getTotalBalance().map { it ?: 0L }
    }
}

