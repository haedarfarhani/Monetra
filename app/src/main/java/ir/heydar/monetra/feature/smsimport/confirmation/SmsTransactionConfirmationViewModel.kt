package ir.heydar.monetra.feature.smsimport.confirmation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsTransactionConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingDao: PendingBankTransactionDao,
    private val confirmUseCase: ConfirmBankTransactionUseCase,
    private val rejectUseCase: RejectBankTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val pendingId: Long = savedStateHandle.get<Long>("pendingId") ?: -1L

    private val _uiState = MutableStateFlow(SmsTransactionConfirmationUiState())
    val uiState: StateFlow<SmsTransactionConfirmationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (pendingId <= 0L) {
                _uiState.update { it.copy(status = ConfirmationStatus.ERROR, errorMessage = "Invalid transaction ID") }
                return@launch
            }

            val pending = pendingDao.getById(pendingId)
            if (pending == null) {
                _uiState.update { it.copy(status = ConfirmationStatus.ERROR, errorMessage = "Transaction not found") }
                return@launch
            }

            if (pending.status == PendingStatus.CONFIRMED.name) {
                _uiState.update { it.copy(status = ConfirmationStatus.CONFIRMED, pending = pending) }
                return@launch
            }

            if (pending.status == PendingStatus.REJECTED.name) {
                _uiState.update { it.copy(status = ConfirmationStatus.REJECTED, pending = pending) }
                return@launch
            }

            if (pending.status == PendingStatus.EXPIRED.name || pending.expiresAt < System.currentTimeMillis()) {
                _uiState.update { it.copy(status = ConfirmationStatus.EXPIRED, pending = pending) }
                return@launch
            }

            val parsedType = try {
                TransactionType.valueOf(pending.transactionType)
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }

            combine(
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories()
            ) { accounts, categories ->
                accounts to categories
            }.collect { (accounts, categories) ->
                val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()

                _uiState.update { current ->
                    current.copy(
                        pending = pending,
                        status = ConfirmationStatus.READY,
                        accounts = accounts,
                        categories = categories,
                        selectedAccountId = current.selectedAccountId ?: defaultAccount?.id,
                        description = current.description.ifBlank { pending.description.orEmpty() },
                        type = parsedType
                    )
                }
            }
        }
    }

    fun onEvent(event: SmsTransactionConfirmationUiEvent) {
        when (event) {
            is SmsTransactionConfirmationUiEvent.SelectAccount -> {
                _uiState.update { it.copy(selectedAccountId = event.accountId) }
            }
            is SmsTransactionConfirmationUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
            }
            is SmsTransactionConfirmationUiEvent.ChangeDescription -> {
                _uiState.update { it.copy(description = event.value) }
            }
            is SmsTransactionConfirmationUiEvent.ChangeType -> {
                _uiState.update { it.copy(type = event.type, selectedCategoryId = null) }
            }
            SmsTransactionConfirmationUiEvent.Confirm -> confirm()
            SmsTransactionConfirmationUiEvent.Reject -> reject()
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
                _uiState.update { it.copy(isProcessing = false, status = ConfirmationStatus.CONFIRMED) }
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
            _uiState.update { it.copy(isProcessing = false, status = ConfirmationStatus.REJECTED) }
        }
    }
}
