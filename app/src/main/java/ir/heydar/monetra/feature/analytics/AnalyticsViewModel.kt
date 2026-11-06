package ir.heydar.monetra.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    /** Kept as a flow so changing the range re-emits through combine without a loading flash. */
    private val selectedRange = MutableStateFlow(AnalyticsRange.THIS_MONTH)

    init {
        loadData()
    }

    fun selectRange(range: AnalyticsRange) {
        selectedRange.value = range
    }

    /** Purely presentational switch — the trend lists are already in the state. */
    fun selectGranularity(granularity: AnalyticsGranularity) {
        _uiState.update { it.copy(selectedGranularity = granularity) }
    }

    private fun loadData() {
        viewModelScope.launch(dispatcherProvider.io) {
            val preferencesFlow = combine(
                preferencesManager.currency,
                preferencesManager.calendarSystem,
                preferencesManager.language
            ) { currencyCode, calendarKey, language ->
                PreferencesBundle(currencyCode, calendarKey, language)
            }

            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                preferencesFlow,
                selectedRange
            ) { transactions, categories, prefs, range ->
                val calendarSystem = CalendarSystem.fromKey(prefs.calendarKey)
                val locale = resolveLocale(prefs.language)
                val currency = AppCurrency.fromCode(prefs.currencyCode)
                val now = System.currentTimeMillis()
                val start = AnalyticsCalculator.rangeStart(range, now, calendarSystem)

                val totals = AnalyticsCalculator.computeRangeTotals(transactions, start, now, now)
                // For ALL_TIME the daily view shows the last year only; longer spans are unreadable.
                val dailyStart = if (range == AnalyticsRange.ALL_TIME) {
                    maxOf(start, now - MAX_DAILY_WINDOW_DAYS)
                } else {
                    start
                }
                val dailyPoints = AnalyticsCalculator.buildDailyPoints(
                    transactions = transactions,
                    startMillis = dailyStart,
                    endMillis = now,
                    now = now,
                    calendar = calendarSystem,
                    locale = locale
                )
                val monthsCount = when (range) {
                    AnalyticsRange.THIS_MONTH -> 6
                    AnalyticsRange.LAST_3_MONTHS -> 6
                    AnalyticsRange.THIS_YEAR -> 12
                    AnalyticsRange.ALL_TIME -> 12
                }
                val monthlyPoints = AnalyticsCalculator.buildMonthlyPoints(
                    transactions, now, calendarSystem, locale, monthsCount
                )
                val yearlyPoints = AnalyticsCalculator.buildYearlyPoints(
                    transactions, now, calendarSystem, locale, YEARS_COUNT
                )
                val averages = AnalyticsCalculator.computeAverages(
                    transactions, range, now, calendarSystem, monthlyPoints
                )
                val categoryBreakdown = AnalyticsCalculator.buildCategoryBreakdown(
                    transactions, categories, start, now, now
                )
                val insights = AnalyticsCalculator.computeInsights(
                    transactions, range, now, calendarSystem,
                    dailyPoints, categoryBreakdown, categories, averages
                )
                val comparison = AnalyticsCalculator.computeComparison(
                    transactions, range, now, calendarSystem
                )

                AnalyticsUiState(
                    isLoading = false,
                    selectedRange = range,
                    selectedGranularity = _uiState.value.selectedGranularity,
                    totalIncome = totals.income,
                    totalExpense = totals.expense,
                    netSavings = totals.income - totals.expense,
                    comparison = comparison,
                    dailyPoints = dailyPoints,
                    monthlyPoints = monthlyPoints,
                    yearlyPoints = yearlyPoints,
                    averages = averages,
                    transactionsCount = totals.count,
                    categoryBreakdown = categoryBreakdown,
                    insights = insights,
                    currency = currency,
                    calendar = calendarSystem,
                    language = prefs.language
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun resolveLocale(language: String): Locale =
        if (language.isBlank()) Locale.getDefault() else Locale.forLanguageTag(language)

    private data class PreferencesBundle(
        val currencyCode: String,
        val calendarKey: String,
        val language: String
    )

    private companion object {
        const val YEARS_COUNT = 5
        const val MAX_DAILY_WINDOW_DAYS = 365L * 24 * 60 * 60 * 1000
    }
}
