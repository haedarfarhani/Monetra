package ir.heydar.monetra.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.repository.AccountRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Roles selectable from the account editor sheet. */
const val ROLE_NONE = "NONE"
const val ROLE_DEFAULT = "DEFAULT"
const val ROLE_DAILY = "DAILY"
const val ROLE_SALARY = "SALARY"

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AccountsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        observeAccounts()
    }

    fun onEvent(event: AccountsUiEvent) {
        when (event) {
            is AccountsUiEvent.Refresh -> Unit
            is AccountsUiEvent.AddAccountClicked -> _uiState.update {
                it.copy(isSheetVisible = true, editingAccount = null)
            }
            is AccountsUiEvent.DismissSheet -> _uiState.update {
                it.copy(isSheetVisible = false, editingAccount = null)
            }
            is AccountsUiEvent.EditAccountClicked -> _uiState.update { state ->
                state.copy(
                    isSheetVisible = true,
                    editingAccount = state.accounts.firstOrNull { it.id == event.id }
                )
            }
            is AccountsUiEvent.SubmitAccount -> submitAccount(event)
            is AccountsUiEvent.DeleteRequested -> _uiState.update { state ->
                state.copy(accountToDelete = state.accounts.firstOrNull { it.id == event.id })
            }
            is AccountsUiEvent.DismissDeleteConfirm -> _uiState.update {
                it.copy(accountToDelete = null)
            }
            is AccountsUiEvent.DeleteConfirmed -> deleteAccount()
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                accountRepository.getAllAccounts(),
                combine(
                    preferencesManager.currency,
                    preferencesManager.language
                ) { currency, language -> currency to language }
            ) { accounts, preferences ->
                // The headline balance is the default account's balance;
                // fall back to the first account while no default is set.
                val totalBalance = accounts.firstOrNull { it.isDefault }?.balance
                    ?: accounts.firstOrNull()?.balance
                    ?: 0L
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        totalBalance = totalBalance,
                        defaultCurrency = AppCurrency.fromCode(preferences.first),
                        language = preferences.second,
                        error = null
                    )
                }
            }.collect { }
        }
    }

    private fun submitAccount(event: AccountsUiEvent.SubmitAccount) {
        val name = event.name.trim()
        if (name.isEmpty() || _uiState.value.isSaving) return

        val editing = _uiState.value.editingAccount
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isSaving = true) }

            runCatching {
                if (editing == null) {
                    createAccount(name, event)
                } else {
                    updateAccount(editing, name, event)
                }
                clearCompetingRoles(event.role, keepId = editing?.id ?: 0L)
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false, isSheetVisible = false, editingAccount = null)
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.send(AccountsUiEffect.ShowMessage(throwable.message ?: "Operation failed"))
            }
        }
    }

    private suspend fun createAccount(name: String, event: AccountsUiEvent.SubmitAccount) {
        val initialBalance = MoneyFormatter
            .parseToMinorUnits(event.initialBalanceInput.ifBlank { "0" }, event.currency)
            ?: 0L
        val accounts = _uiState.value.accounts
        // The very first account becomes the default unless the user picked a role.
        val effectiveRole = if (event.role == ROLE_NONE && accounts.isEmpty()) ROLE_DEFAULT else event.role

        accountRepository.insertAccount(
            Account(
                name = name,
                type = event.type,
                balance = initialBalance,
                initialBalance = initialBalance,
                currency = event.currency.code,
                isDefault = effectiveRole == ROLE_DEFAULT,
                purpose = purposeOf(effectiveRole),
                bankName = event.bankName.trim(),
                accountNumber = event.accountNumber.trim(),
                cardNumber = event.cardNumber.trim(),
                holderName = event.holderName.trim()
            )
        )
    }

    private suspend fun updateAccount(editing: Account, name: String, event: AccountsUiEvent.SubmitAccount) {
        accountRepository.updateAccount(
            editing.copy(
                name = name,
                type = event.type,
                isDefault = event.role == ROLE_DEFAULT,
                purpose = purposeOf(event.role),
                bankName = event.bankName.trim(),
                accountNumber = event.accountNumber.trim(),
                cardNumber = event.cardNumber.trim(),
                holderName = event.holderName.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Keeps at most one default / daily / salary account by clearing the role elsewhere. */
    private suspend fun clearCompetingRoles(role: String, keepId: Long) {
        val accounts = _uiState.value.accounts
        when (role) {
            ROLE_DEFAULT -> accounts
                .filter { it.id != keepId && it.isDefault }
                .forEach { accountRepository.updateAccount(it.copy(isDefault = false)) }

            ROLE_DAILY -> accounts
                .filter { it.id != keepId && it.purpose == Account.PURPOSE_DAILY }
                .forEach { accountRepository.updateAccount(it.copy(purpose = "")) }

            ROLE_SALARY -> accounts
                .filter { it.id != keepId && it.purpose == Account.PURPOSE_SALARY }
                .forEach { accountRepository.updateAccount(it.copy(purpose = "")) }
        }
    }

    private fun purposeOf(role: String): String = when (role) {
        ROLE_DAILY -> Account.PURPOSE_DAILY
        ROLE_SALARY -> Account.PURPOSE_SALARY
        else -> ""
    }

    private fun deleteAccount() {
        val account = _uiState.value.accountToDelete ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            runCatching {
                accountRepository.deleteAccount(account)
                // Keep at least one default account around.
                val remaining = _uiState.value.accounts.filter { it.id != account.id }
                if (account.isDefault && remaining.isNotEmpty()) {
                    accountRepository.updateAccount(
                        remaining.first().copy(isDefault = true, updatedAt = System.currentTimeMillis())
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(accountToDelete = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(accountToDelete = null) }
                _uiEffect.send(AccountsUiEffect.ShowMessage(throwable.message ?: "Could not delete account"))
            }
        }
    }
}
