package ir.heydar.monetra.feature.bankreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.data.local.dao.PendingBankTransactionDao
import ir.heydar.monetra.data.local.entity.PendingStatus
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.usecase.ConfirmBankTransactionUseCase
import ir.heydar.monetra.domain.usecase.RejectBankTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankTransactionReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingDao: PendingBankTransactionDao,
    private val confirmUseCase: ConfirmBankTransactionUseCase,
    private val rejectUseCase: RejectBankTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val pendingId: Long = savedStateHandle.get<Long>("pendingId") ?: -1L

    private val _uiState = MutableStateFlow(BankTransactionReviewUiState())
    val uiState: StateFlow<BankTransactionReviewUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (pendingId <= 0L) {
                _uiState.update { it.copy(status = ReviewScreenStatus.ERROR, errorMessage = "Invalid transaction ID") }
                return@launch
            }

            val pending = pendingDao.getById(pendingId)
            if (pending == null) {
                _uiState.update { it.copy(status = ReviewScreenStatus.ERROR, errorMessage = "Transaction not found") }
                return@launch
            }

            if (pending.status == PendingStatus.CONFIRMED.name) {
                _uiState.update { it.copy(status = ReviewScreenStatus.CONFIRMED, pending = pending) }
                return@launch
            }

            if (pending.status == PendingStatus.REJECTED.name) {
                _uiState.update { it.copy(status = ReviewScreenStatus.REJECTED, pending = pending) }
                return@launch
            }

            if (pending.status == PendingStatus.EXPIRED.name || pending.expiresAt < System.currentTimeMillis()) {
                _uiState.update { it.copy(status = ReviewScreenStatus.EXPIRED, pending = pending) }
                return@launch
            }

            val currency = AppCurrency.fromCode(pending.currencyCode)
            val parsedType = try {
                TransactionType.valueOf(pending.transactionType)
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

            combine(
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories(),
                preferencesManager.calendarSystem,
                preferencesManager.language
            ) { accounts, categories, calendar, language ->
                Quadruple(accounts, categories, CalendarSystem.fromKey(calendar), language)
            }.collect { (accounts, categories, calendar, language) ->
                val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()

                // Auto-suggest category if enabled
                val autoSuggestCategory = preferencesManager.bankNotificationsAutoSuggestCategory.first()
                val suggestedCategoryId = if (autoSuggestCategory && !pending.description.isNullOrBlank()) {
                    categories.firstOrNull { cat ->
                        cat.type == parsedType && pending.description.contains(cat.name, ignoreCase = true)
                    }?.id
                } else null

                _uiState.update { current ->
                    current.copy(
                        pending = pending,
                        status = ReviewScreenStatus.READY,
                        accounts = accounts,
                        categories = categories,
                        selectedAccountId = current.selectedAccountId ?: defaultAccount?.id,
                        selectedCategoryId = current.selectedCategoryId ?: suggestedCategoryId,
                        description = current.description.ifBlank { pending.description.orEmpty() },
                        amountInput = MoneyFormatter.format(pending.amountMinorUnits, currency),
                        type = parsedType,
                        currency = currency,
                        calendar = calendar,
                        language = language
                    )
                }
            }
        }
    }

    fun onEvent(event: BankTransactionReviewUiEvent) {
        when (event) {
            is BankTransactionReviewUiEvent.SelectAccount -> {
                _uiState.update { it.copy(selectedAccountId = event.accountId) }
            }
            is BankTransactionReviewUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
            }
            is BankTransactionReviewUiEvent.ChangeDescription -> {
                _uiState.update { it.copy(description = event.value) }
            }
            is BankTransactionReviewUiEvent.ChangeType -> {
                _uiState.update { it.copy(type = event.type, selectedCategoryId = null) }
            }
            is BankTransactionReviewUiEvent.Confirm -> confirm()
            is BankTransactionReviewUiEvent.Reject -> reject()
        }
    }

    private fun confirm() {
        val state = _uiState.value
        val pending = state.pending ?: return
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = confirmUseCase(
                pendingId = pending.id,
                overrideAccountId = state.selectedAccountId,
                overrideCategoryId = state.selectedCategoryId,
                overrideDescription = state.description.trim().ifBlank { null }
            )
            result.onSuccess {
                _uiState.update { it.copy(isProcessing = false, status = ReviewScreenStatus.CONFIRMED) }
            }.onFailure { err ->
                _uiState.update { it.copy(isProcessing = false, errorMessage = err.message) }
            }
        }
    }

    private fun reject() {
        val pending = _uiState.value.pending ?: return
        if (_uiState.value.isProcessing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            rejectUseCase(pending.id)
            _uiState.update { it.copy(isProcessing = false, status = ReviewScreenStatus.REJECTED) }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
