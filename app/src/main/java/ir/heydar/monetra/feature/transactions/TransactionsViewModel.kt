package ir.heydar.monetra.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionFilter
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private data class Sources(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currency: AppCurrency = AppCurrency.USD,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en"
)

private data class PagingState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val filters = MutableStateFlow(TransactionFilter())
    private val loadedItems = MutableStateFlow<List<Transaction>>(emptyList())
    private val paging = MutableStateFlow(PagingState())
    private val sources = MutableStateFlow(Sources())

    private val _uiEffect = Channel<TransactionsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        sources,
        loadedItems,
        filters,
        paging
    ) { source, items, filter, page ->
        TransactionsUiState(
            isLoading = page.isLoading,
            isLoadingMore = page.isLoadingMore,
            hasMore = page.hasMore,
            groups = groupByDate(items, source),
            accounts = source.accounts,
            categories = source.categories,
            search = filter.query,
            typeFilter = filter.type,
            accountFilter = filter.accountId,
            categoryFilter = filter.categoryId,
            startMillis = filter.startMillis.takeIf { it > 0L },
            endMillis = filter.endMillis.takeIf { it < Long.MAX_VALUE },
            currency = source.currency,
            calendar = source.calendar,
            language = source.language,
            error = page.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState()
    )

    init {
        observeSources()
        observeFilters()
    }

    fun onEvent(event: TransactionsUiEvent) {
        when (event) {
            is TransactionsUiEvent.DeleteTransaction -> {
                viewModelScope.launch(dispatcherProvider.io) {
                    runCatching { transactionRepository.deleteTransaction(event.id) }
                }
            }
            is TransactionsUiEvent.Refresh -> viewModelScope.launch(dispatcherProvider.io) { reload() }
            is TransactionsUiEvent.SearchChanged -> filters.update { it.copy(query = event.query) }
            is TransactionsUiEvent.TypeFilterChanged -> filters.update { it.copy(type = event.type) }
            is TransactionsUiEvent.AccountFilterChanged -> filters.update { it.copy(accountId = event.accountId) }
            is TransactionsUiEvent.CategoryFilterChanged -> filters.update { it.copy(categoryId = event.categoryId) }
            is TransactionsUiEvent.DateRangeChanged -> filters.update {
                it.copy(
                    startMillis = event.startMillis ?: 0L,
                    endMillis = event.endMillis ?: Long.MAX_VALUE
                )
            }
            is TransactionsUiEvent.ClearFilters -> filters.value = TransactionFilter()
            is TransactionsUiEvent.LoadMore -> loadMore()
            is TransactionsUiEvent.TransactionClicked -> {
                viewModelScope.launch { _uiEffect.send(TransactionsUiEffect.NavigateToDetail(event.id)) }
            }
            is TransactionsUiEvent.AddClicked -> {
                viewModelScope.launch { _uiEffect.send(TransactionsUiEffect.NavigateToAdd) }
            }
        }
    }

    private fun observeSources() {
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories(),
                combine(
                    preferencesManager.currency,
                    preferencesManager.calendarSystem,
                    preferencesManager.language
                ) { currency, calendar, language ->
                    Triple(currency, CalendarSystem.fromKey(calendar), language)
                }
            ) { accounts, categories, prefs ->
                Sources(
                    accounts = accounts,
                    categories = categories,
                    currency = AppCurrency.fromCode(prefs.first),
                    calendar = prefs.second,
                    language = prefs.third
                )
            }.collect { sources.value = it }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch(dispatcherProvider.io) {
            filters.collect { reload() }
        }
    }

    private suspend fun reload() {
        paging.update { it.copy(isLoading = true, error = null) }
        runCatching {
            transactionRepository.searchTransactions(
                filters.value.copy(limit = PAGE_SIZE, offset = 0)
            )
        }.onSuccess { page ->
            loadedItems.value = page
            paging.update { it.copy(isLoading = false, hasMore = page.size == PAGE_SIZE) }
        }.onFailure { throwable ->
            paging.update { it.copy(isLoading = false, error = throwable.message) }
        }
    }

    private fun loadMore() {
        val current = paging.value
        if (current.isLoadingMore || !current.hasMore || current.isLoading) return
        viewModelScope.launch(dispatcherProvider.io) {
            paging.update { it.copy(isLoadingMore = true) }
            runCatching {
                transactionRepository.searchTransactions(
                    filters.value.copy(limit = PAGE_SIZE, offset = loadedItems.value.size)
                )
            }.onSuccess { page ->
                loadedItems.value = loadedItems.value + page
                paging.update { it.copy(isLoadingMore = false, hasMore = page.size == PAGE_SIZE) }
            }.onFailure { throwable ->
                paging.update { it.copy(isLoadingMore = false, error = throwable.message) }
            }
        }
    }

    private fun groupByDate(items: List<Transaction>, source: Sources): List<TransactionGroup> {
        val locale = Locale.forLanguageTag(source.language)
        return items
            .groupBy { AppDateFormatter.formatDate(it.timestamp, source.calendar, locale) }
            .map { (label, transactions) ->
                TransactionGroup(
                    dateLabel = label,
                    incomeMinorUnits = transactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amountMinorUnits },
                    expenseMinorUnits = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountMinorUnits },
                    transactions = transactions
                )
            }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
