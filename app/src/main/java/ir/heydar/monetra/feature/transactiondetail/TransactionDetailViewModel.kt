package ir.heydar.monetra.feature.transactiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: -1L

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<TransactionDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        observe()
    }

    fun onEvent(event: TransactionDetailUiEvent) {
        when (event) {
            TransactionDetailUiEvent.Edit -> viewModelScope.launch {
                _uiEffect.send(TransactionDetailUiEffect.NavigateToEdit)
            }
            TransactionDetailUiEvent.Delete -> viewModelScope.launch {
                runCatching { transactionRepository.deleteTransaction(transactionId) }
                    .onSuccess { _uiEffect.send(TransactionDetailUiEffect.Deleted) }
                    .onFailure { throwable ->
                        _uiState.update { it.copy(error = throwable.message) }
                    }
            }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionById(transactionId),
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories(),
                combine(
                    preferencesManager.calendarSystem,
                    preferencesManager.language
                ) { calendar, language -> CalendarSystem.fromKey(calendar) to language }
            ) { transaction, accounts, categories, prefs ->
                TransactionDetailUiState(
                    isLoading = false,
                    transaction = transaction,
                    accountName = accounts.firstOrNull { it.id == transaction?.accountId }?.name,
                    counterAccountName = accounts.firstOrNull { it.id == transaction?.counterAccountId }?.name,
                    categoryName = categories.firstOrNull { it.id == transaction?.categoryId }?.name,
                    currency = AppCurrency.fromCode(transaction?.currencyCode),
                    calendar = prefs.first,
                    language = prefs.second,
                    error = if (transaction == null) "Transaction not found" else null
                )
            }.collect { _uiState.value = it }
        }
    }
}
