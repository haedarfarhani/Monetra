package ir.heydar.monetra.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private data class DashboardPreferences(
    val currencyCode: String,
    val calendar: CalendarSystem,
    val language: String
)

private data class DashboardSummary(
    val income: Long,
    val expense: Long,
    val trend: List<MonthlyTrendPoint>,
    val topCategories: List<CategorySpending>,
    val daysInMonth: Int,
    val dayOfMonth: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<DashboardUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        loadDashboardData()
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.Refresh -> loadDashboardData()
            is DashboardUiEvent.AddTransactionClicked -> {
                viewModelScope.launch { _uiEffect.send(DashboardUiEffect.NavigateToAddTransaction) }
            }
            is DashboardUiEvent.TogglePrivacy -> {
                _uiState.update { it.copy(isPrivacyEnabled = !it.isPrivacyEnabled) }
            }
            is DashboardUiEvent.TransactionClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(DashboardUiEffect.NavigateToTransactionDetail(event.transactionId))
                }
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val preferences = combine(
                preferencesManager.currency,
                preferencesManager.calendarSystem,
                preferencesManager.language
            ) { currency, calendar, language ->
                DashboardPreferences(currency, CalendarSystem.fromKey(calendar), language)
            }

            val monthlyIncome: Flow<Long> = preferences.flatMapLatest { prefs ->
                val range = AppDateFormatter.monthRange(System.currentTimeMillis(), prefs.calendar)
                transactionRepository.getTotalIncome(range.first, range.last)
            }

            val monthlyExpense: Flow<Long> = preferences.flatMapLatest { prefs ->
                val range = AppDateFormatter.monthRange(System.currentTimeMillis(), prefs.calendar)
                transactionRepository.getTotalExpense(range.first, range.last)
            }

            // Trend chart + this month's top spending categories, built from the
            // same transaction source the reports screen uses so both agree.
            val analytics: Flow<DashboardSummary> = preferences.flatMapLatest { prefs ->
                combine(
                    transactionRepository.getAllTransactions(),
                    categoryRepository.getAllCategories()
                ) { transactions, categories ->
                    val now = System.currentTimeMillis()
                    val monthRange = AppDateFormatter.monthRange(now, prefs.calendar)
                    DashboardSummary(
                        income = 0L,
                        expense = 0L,
                        trend = buildMonthlyTrend(
                            transactions = transactions,
                            calendar = prefs.calendar,
                            locale = Locale.forLanguageTag(prefs.language)
                        ),
                        topCategories = buildTopCategories(transactions, categories, monthRange),
                        daysInMonth = monthRange.dayCount(),
                        dayOfMonth = (((now - monthRange.first) / DAY_MS) + 1)
                            .toInt()
                            .coerceIn(1, monthRange.dayCount())
                    )
                }
            }

            val summary = combine(monthlyIncome, monthlyExpense, analytics) { income, expense, data ->
                data.copy(income = income, expense = expense)
            }

            combine(
                transactionRepository.getRecentTransactions(limit = 5),
                accountRepository.getAllAccounts(),
                preferences,
                summary
            ) { recentTransactions, accounts, prefs, summaryData ->
                // The headline balance comes from the default account only;
                // fall back to the first account while no default is set.
                val totalBalance = accounts.firstOrNull { it.isDefault }?.balance
                    ?: accounts.firstOrNull()?.balance
                    ?: 0L
                DashboardUiState(
                    isLoading = false,
                    totalBalance = totalBalance,
                    monthlyIncome = summaryData.income,
                    monthlyExpense = summaryData.expense,
                    monthlyTrend = summaryData.trend,
                    recentTransactions = recentTransactions,
                    accounts = accounts,
                    topCategories = summaryData.topCategories,
                    daysInMonth = summaryData.daysInMonth,
                    dayOfMonth = summaryData.dayOfMonth,
                    isPrivacyEnabled = _uiState.value.isPrivacyEnabled,
                    currencyCode = prefs.currencyCode,
                    calendar = prefs.calendar,
                    language = prefs.language,
                    error = null
                )
            }.catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load dashboard data"
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Four most recent months, from oldest to newest, using the month boundaries of the selected
     * calendar so Jalali months are grouped correctly.
     */
    private fun buildMonthlyTrend(
        transactions: List<Transaction>,
        calendar: CalendarSystem,
        locale: Locale
    ): List<MonthlyTrendPoint> {
        val monthStarts = ArrayDeque<Long>()
        var anchor = System.currentTimeMillis()
        repeat(TREND_MONTHS) {
            val range = AppDateFormatter.monthRange(anchor, calendar)
            monthStarts.addFirst(range.first)
            anchor = range.first - 1
        }

        return monthStarts.map { monthStart ->
            val range = AppDateFormatter.monthRange(monthStart, calendar)
            val monthTransactions = transactions.filter { it.timestamp in range.first..range.last }
            MonthlyTrendPoint(
                label = monthLabel(monthStart, calendar, locale),
                income = monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountMinorUnits },
                expense = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountMinorUnits }
            )
        }
    }

    /**
     * The categories the user spent the most in this month, with each one's share
     * of the total monthly expense.
     */
    private fun buildTopCategories(
        transactions: List<Transaction>,
        categories: List<ir.heydar.monetra.domain.model.Category>,
        monthRange: LongRange
    ): List<CategorySpending> {
        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.timestamp in monthRange.first..monthRange.last
        }
        val totalExpense = monthExpenses.sumOf { it.amountMinorUnits }
        if (totalExpense <= 0L) return emptyList()

        return monthExpenses
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .map { (categoryId, categoryTransactions) ->
                val amount = categoryTransactions.sumOf { it.amountMinorUnits }
                CategorySpending(
                    categoryId = categoryId,
                    name = categories.firstOrNull { it.id == categoryId }?.name.orEmpty(),
                    amount = amount,
                    percentage = amount.toFloat() / totalExpense
                )
            }
            .sortedByDescending { it.amount }
            .take(TOP_CATEGORIES)
    }

    private fun monthLabel(timestamp: Long, calendar: CalendarSystem, locale: Locale): String =
        when (calendar) {
            CalendarSystem.JALALI -> PersianDate(timestamp).monthName()
            CalendarSystem.GREGORIAN -> SimpleDateFormat("MMM", locale).format(Date(timestamp))
        }

    private companion object {
        const val TREND_MONTHS = 4
        const val TOP_CATEGORIES = 4
    }
}

private const val DAY_MS = 86_400_000L

/** Number of whole days covered by a month range. */
private fun LongRange.dayCount(): Int =
    (((last - first) / DAY_MS) + 1).toInt().coerceAtLeast(1)
