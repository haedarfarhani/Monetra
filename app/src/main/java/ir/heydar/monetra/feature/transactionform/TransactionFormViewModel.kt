package ir.heydar.monetra.feature.transactionform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<Long>(TRANSACTION_ID_ARG) ?: -1L

    // Tab the form opens on for NEW transactions. Navigation stores enum args as strings in
    // some versions, so accept both the enum and its name. Overridden when editing below.
    private val initialType: TransactionType = resolveInitialType(savedStateHandle)

    // The merged form only edits `description`; original notes/reference of an existing
    // transaction are preserved untouched so editing never wipes stored data.
    private var preservedNotes: String? = null
    private var preservedReference: String? = null

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(
            isEditing = editingId > 0,
            // New entries start on the requested tab; editing loads the stored type instead.
            type = if (editingId > 0) TransactionType.EXPENSE else initialType
        )
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<TransactionFormUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        observePreferences()
        observeAccountsAndCategories()
        if (editingId > 0) loadExisting(editingId)
    }

    fun onEvent(event: TransactionFormUiEvent) {
        when (event) {
            is TransactionFormUiEvent.ChangeType -> _uiState.update {
                it.copy(
                    type = event.type,
                    categoryId = null,
                    categoryError = false,
                    counterAccountId = if (event.type == TransactionType.TRANSFER) it.counterAccountId else null
                )
            }

            is TransactionFormUiEvent.ChangeAmount -> _uiState.update {
                it.copy(amountInput = event.value, amountError = false)
            }

            is TransactionFormUiEvent.SelectAccount -> _uiState.update { state ->
                val account = state.accounts.firstOrNull { it.id == event.accountId }
                state.copy(
                    accountId = event.accountId,
                    currency = account?.let { AppCurrency.fromCode(it.currency) } ?: state.currency,
                    accountError = false
                )
            }

            is TransactionFormUiEvent.SelectCounterAccount -> _uiState.update {
                it.copy(counterAccountId = event.accountId, counterAccountError = false)
            }

            is TransactionFormUiEvent.SelectCategory -> _uiState.update {
                it.copy(categoryId = event.categoryId, categoryError = false)
            }

            is TransactionFormUiEvent.ChangeDate -> _uiState.update {
                it.copy(timestamp = mergeDate(it.timestamp, event.utcMillis))
            }

            is TransactionFormUiEvent.ChangeTime -> _uiState.update {
                it.copy(timestamp = mergeTime(it.timestamp, event.hour, event.minute))
            }

            is TransactionFormUiEvent.ChangeDescription -> _uiState.update { it.copy(description = event.value) }

            is TransactionFormUiEvent.CreateCategory -> createCategory(event.name)

            is TransactionFormUiEvent.CreateAccount -> createAccount(event)

            TransactionFormUiEvent.Save -> save()

            TransactionFormUiEvent.Delete -> delete()
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferencesManager.currency,
                preferencesManager.calendarSystem,
                preferencesManager.language
            ) { currency, calendar, language ->
                Triple(currency, CalendarSystem.fromKey(calendar), language)
            }.collect { (currency, calendar, language) ->
                _uiState.update { state ->
                    state.copy(
                        calendar = calendar,
                        language = language,
                        currency = if (state.isEditing) state.currency else AppCurrency.fromCode(currency)
                    )
                }
            }
        }
    }

    private fun observeAccountsAndCategories() {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories()
            ) { accounts, categories -> accounts to categories }
                .collect { (accounts, categories) ->
                    _uiState.update { state ->
                        val resolvedAccountId = state.accountId
                            ?: preferredAccountIdFor(state.type, accounts)
                        val account = accounts.firstOrNull { it.id == resolvedAccountId }
                        state.copy(
                            accounts = accounts,
                            categories = categories,
                            accountId = resolvedAccountId,
                            counterAccountId = state.counterAccountId
                                ?.takeIf { id -> accounts.any { it.id == id } },
                            currency = account?.let { AppCurrency.fromCode(it.currency) } ?: state.currency,
                            isLoading = state.isEditing && state.isLoading
                        )
                    }
                }
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val existing = transactionRepository.getTransactionById(id).first()
            if (existing == null) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.send(TransactionFormUiEffect.Failed("Transaction not found"))
                return@launch
            }
            _uiState.update { state ->
                val existingDescription = existing.description.orEmpty()
                val existingNotes = existing.notes.orEmpty()
                // With a single merged field, fall back to notes when the description is empty.
                preservedNotes = if (existingDescription.isBlank() && existingNotes.isNotBlank()) {
                    null
                } else {
                    existing.notes
                }
                preservedReference = existing.referenceNumber
                state.copy(
                    isLoading = false,
                    isEditing = true,
                    type = existing.type,
                    amountInput = MoneyFormatter.formatNumber(
                        existing.amountMinorUnits,
                        AppCurrency.fromCode(existing.currencyCode)
                    ).replace(",", "").replace(THOUSANDS_SEPARATOR, ""),
                    currency = AppCurrency.fromCode(existing.currencyCode),
                    accountId = existing.accountId,
                    counterAccountId = existing.counterAccountId,
                    categoryId = existing.categoryId,
                    timestamp = existing.timestamp,
                    description = existingDescription.ifBlank { existingNotes }
                )
            }
        }
    }

    private fun createAccount(event: TransactionFormUiEvent.CreateAccount) {
        val name = event.name.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            val state = _uiState.value
            val initialBalance = MoneyFormatter
                .parseToMinorUnits(event.initialBalanceInput.ifBlank { "0" }, state.currency)
                ?: 0L

            runCatching {
                accountRepository.insertAccount(
                    Account(
                        name = name,
                        type = event.accountType,
                        balance = initialBalance,
                        initialBalance = initialBalance,
                        currency = state.currency.code,
                        isDefault = state.accounts.isEmpty()
                    )
                )
            }.onSuccess { newAccountId ->
                // Select the new account so the transaction can be saved without extra taps.
                _uiState.update {
                    it.copy(accountId = newAccountId, accountError = false)
                }
            }.onFailure { throwable ->
                _uiEffect.send(
                    TransactionFormUiEffect.Failed(throwable.message ?: "Could not create account")
                )
            }
        }
    }

    private fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = categoryRepository.createCategory(
                Category(name = trimmed, type = _uiState.value.type)
            )
            _uiState.update { it.copy(categoryId = id) }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = MoneyFormatter.parseToMinorUnits(state.amountInput, state.currency)
        val amountValid = amount != null && amount > 0L
        val accountValid = state.accountId != null
        val counterValid = !state.isTransfer ||
            (state.counterAccountId != null && state.counterAccountId != state.accountId)
        val categoryValid = state.isTransfer || state.categoryId != null

        if (!amountValid || !accountValid || !counterValid || !categoryValid) {
            _uiState.update {
                it.copy(
                    amountError = !amountValid,
                    accountError = !accountValid,
                    counterAccountError = !counterValid,
                    categoryError = !categoryValid
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val transaction = Transaction(
                id = if (state.isEditing) editingId else 0L,
                accountId = requireNotNull(state.accountId),
                counterAccountId = state.counterAccountId.takeIf { state.isTransfer },
                categoryId = state.categoryId.takeIf { !state.isTransfer },
                amountMinorUnits = requireNotNull(amount),
                currencyCode = state.currency.code,
                type = state.type,
                description = state.description.trim().ifBlank { null },
                notes = preservedNotes,
                referenceNumber = preservedReference,
                transferGroupId = if (state.isTransfer) {
                    UUID.randomUUID().toString()
                } else {
                    null
                },
                timestamp = state.timestamp,
                updatedAt = System.currentTimeMillis()
            )

            runCatching {
                if (state.isEditing) {
                    transactionRepository.updateTransaction(transaction)
                } else {
                    transactionRepository.createTransaction(transaction)
                }
            }.onSuccess {
                _uiEffect.send(TransactionFormUiEffect.Saved)
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.send(TransactionFormUiEffect.Failed(throwable.message ?: "Could not save"))
            }
        }
    }

    private fun delete() {
        if (editingId <= 0) return
        viewModelScope.launch {
            runCatching { transactionRepository.deleteTransaction(editingId) }
                .onSuccess { _uiEffect.send(TransactionFormUiEffect.Deleted) }
                .onFailure { throwable ->
                    _uiEffect.send(TransactionFormUiEffect.Failed(throwable.message ?: "Could not delete"))
                }
        }
    }

    private fun mergeDate(currentTimestamp: Long, utcMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(currentTimestamp).atZone(zone).toLocalTime()
        val date = Instant.ofEpochMilli(utcMillis).atZone(zone).toLocalDate()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    private fun mergeTime(currentTimestamp: Long, hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(currentTimestamp).atZone(zone).toLocalDate()
        return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
    }

    private companion object {
        const val TRANSACTION_ID_ARG = "transactionId"
        const val TRANSACTION_TYPE_ARG = "transactionType"
        const val THOUSANDS_SEPARATOR = "\u066C"

        /** Reads the pre-selected tab from the navigation args, tolerating enum-or-string storage. */
        fun resolveInitialType(savedStateHandle: SavedStateHandle): TransactionType =
            when (val raw = savedStateHandle.get<Any?>(TRANSACTION_TYPE_ARG)) {
                is TransactionType -> raw
                is String -> runCatching { TransactionType.valueOf(raw) }.getOrDefault(TransactionType.EXPENSE)
                else -> TransactionType.EXPENSE
            }
    }
}

/**
 * Picks the account a new transaction should start with: daily-spending account for
 * expenses, salary account for incomes, otherwise the default account.
 */
private fun preferredAccountIdFor(
    type: TransactionType,
    accounts: List<Account>
): Long? = when (type) {
    TransactionType.EXPENSE -> accounts.firstOrNull { it.purpose == Account.PURPOSE_DAILY }?.id
    TransactionType.INCOME -> accounts.firstOrNull { it.purpose == Account.PURPOSE_SALARY }?.id
    TransactionType.TRANSFER -> null
} ?: accounts.firstOrNull { it.isDefault }?.id
    ?: accounts.firstOrNull()?.id
