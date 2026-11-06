package ir.heydar.monetra.feature.smsimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.ParsingStatus
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.SaveImportResult
import ir.heydar.monetra.domain.repository.SmsImportRepository
import ir.heydar.monetra.domain.usecase.CreatePendingBankTransactionUseCase
import ir.heydar.monetra.feature.smsimport.parser.BankMessageParserRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmsImportViewModel @Inject constructor(
    private val registry: BankMessageParserRegistry,
    private val smsImportRepository: SmsImportRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    private val sharedTextStore: SharedTextStore,
    private val createPendingUseCase: CreatePendingBankTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SmsImportUiState(input = sharedTextStore.text.value.orEmpty())
    )
    val uiState: StateFlow<SmsImportUiState> = _uiState.asStateFlow()

    init {
        sharedTextStore.consume()
        observeSources()
        observeAutoSave()
    }

    fun onEvent(event: SmsImportUiEvent) {
        when (event) {
            is SmsImportUiEvent.InputChanged -> _uiState.update {
                it.copy(input = event.value, status = SmsImportStatus.IDLE, draft = null)
            }

            is SmsImportUiEvent.Analyse -> analyse()

            is SmsImportUiEvent.ChangeType -> _uiState.update {
                it.copy(
                    type = event.type,
                    categoryId = null,
                    counterAccountId = if (event.type == TransactionType.TRANSFER) it.counterAccountId else null
                )
            }

            is SmsImportUiEvent.ChangeAmount -> _uiState.update { it.copy(amountInput = event.value) }

            is SmsImportUiEvent.SelectAccount -> _uiState.update { it.copy(accountId = event.accountId) }

            is SmsImportUiEvent.SelectCounterAccount ->
                _uiState.update { it.copy(counterAccountId = event.accountId) }

            is SmsImportUiEvent.SelectCategory -> _uiState.update { it.copy(categoryId = event.categoryId) }

            is SmsImportUiEvent.ChangeDescription -> _uiState.update { it.copy(description = event.value) }

            is SmsImportUiEvent.Save -> save()

            is SmsImportUiEvent.Reject -> reject()
        }
    }

    private fun observeSources() {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories(),
                preferencesManager.currency,
                preferencesManager.calendarSystem,
                preferencesManager.language
            ) { accounts, categories, currency, calendar, language ->
                Sources(
                    accounts = accounts,
                    categories = categories,
                    defaultCurrency = AppCurrency.fromCode(currency),
                    calendar = CalendarSystem.fromKey(calendar),
                    language = language
                )
            }.collect { source ->
                _uiState.update { state ->
                    state.copy(
                        accounts = source.accounts,
                        categories = source.categories,
                        currency = source.defaultCurrency,
                        calendar = source.calendar,
                        language = source.language,
                        accountId = state.accountId
                            ?: source.accounts.firstOrNull { it.isDefault }?.id
                            ?: source.accounts.firstOrNull()?.id
                    )
                }
            }
        }
    }

    private fun observeAutoSave() {
        viewModelScope.launch {
            preferencesManager.autoSaveBankMessages.collect { enabled ->
                _uiState.update { it.copy(autoSaveEnabled = enabled) }
            }
        }
    }

    private fun analyse() {
        val input = _uiState.value.input
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalysing = true, draft = null, status = SmsImportStatus.IDLE) }

            val draft = registry.parse(input)
            val duplicate = draft.fingerprint.isNotBlank() && smsImportRepository.isDuplicate(draft)

            when {
                draft.parsingStatus == ParsingStatus.FAILED || draft.amountMinorUnits == null -> {
                    _uiState.update {
                        it.copy(isAnalysing = false, draft = draft, status = SmsImportStatus.NOT_FINANCIAL)
                    }
                }

                duplicate -> {
                    _uiState.update {
                        it.copy(isAnalysing = false, draft = draft, status = SmsImportStatus.DUPLICATE)
                    }
                }

                else -> {
                    val currency = AppCurrency.fromCode(draft.currencyCode)
                    val amountText = MoneyFormatter.formatNumber(draft.amountMinorUnits, currency)
                        .replace(",", "")

                    _uiState.update {
                        it.copy(
                            isAnalysing = false,
                            draft = draft,
                            status = SmsImportStatus.NEEDS_REVIEW,
                            type = draft.transactionType ?: TransactionType.EXPENSE,
                            currency = currency,
                            amountInput = amountText,
                            description = draft.description.orEmpty()
                        )
                    }

                    val state = _uiState.value
                    if (state.autoSaveEnabled && draft.isHighConfidence && state.accountId != null) {
                        createPendingUseCase(draft)
                        save()
                    }
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val accountId = state.accountId ?: return
        val amount = MoneyFormatter.parseToMinorUnits(state.amountInput, state.currency) ?: return
        if (amount <= 0L || state.isSaving) return
        if (state.isTransfer && state.counterAccountId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val transaction = Transaction(
                accountId = accountId,
                counterAccountId = state.counterAccountId.takeIf { state.isTransfer },
                categoryId = state.categoryId.takeIf { !state.isTransfer },
                amountMinorUnits = amount,
                currencyCode = state.currency.code,
                type = state.type,
                description = state.description.trim().ifBlank { null },
                referenceNumber = draft.referenceNumber,
                source = TransactionSource.BANK_SMS,
                bankName = draft.bankName,
                transferGroupId = if (state.isTransfer) UUID.randomUUID().toString() else null,
                timestamp = draft.transactionTimestamp ?: System.currentTimeMillis()
            )

            runCatching { smsImportRepository.saveDraft(draft, transaction) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            status = when (result) {
                                is SaveImportResult.Saved -> SmsImportStatus.SAVED
                                SaveImportResult.Duplicate -> SmsImportStatus.DUPLICATE
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false, status = SmsImportStatus.FAILED) }
                }
        }
    }

    private fun reject() {
        val draft = _uiState.value.draft
        if (draft == null) {
            _uiState.update { it.copy(input = "", status = SmsImportStatus.IDLE) }
            return
        }
        viewModelScope.launch {
            smsImportRepository.rejectDraft(draft)
            _uiState.update {
                it.copy(input = "", draft = null, status = SmsImportStatus.IDLE)
            }
        }
    }

    private data class Sources(
        val accounts: List<ir.heydar.monetra.domain.model.Account> = emptyList(),
        val categories: List<ir.heydar.monetra.domain.model.Category> = emptyList(),
        val defaultCurrency: AppCurrency = AppCurrency.USD,
        val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
        val language: String = "en"
    )
}
