package ir.heydar.monetra.feature.analytics

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency

enum class AnalyticsRange {
    THIS_MONTH,
    LAST_3_MONTHS,
    THIS_YEAR,
    ALL_TIME
}

/** Bucket size of the trend chart. */
enum class AnalyticsGranularity {
    DAILY,
    MONTHLY,
    YEARLY
}

/**
 * One x-axis point of the income/expense trend charts. [label] is the short axis text and
 * [fullLabel] the richer text shown inside the touch tooltip.
 */
data class FlowPoint(
    val label: String,
    val fullLabel: String = label,
    val income: Long,
    val expense: Long
)

data class CategorySpendData(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val amount: Long,
    val percentage: Float,
    val transactionCount: Int = 0
)

/** Percentage change versus the equivalent previous window, null when no prior data exists. */
data class TrendComparison(
    val incomeChangePct: Float?,
    val expenseChangePct: Float?
)

data class HighestSpendDay(
    val label: String,
    val amount: Long
)

/** Daily/monthly averages and related ratios for the selected range. */
data class AnalyticsAverages(
    val dailyExpense: Long,
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    /** Net savings / income in 0..1, null when there was no income. */
    val savingsRate: Float?
)

/** Narrative insights rendered as small cards on the reports page. */
data class AnalyticsInsights(
    val biggestExpenseAmount: Long = 0L,
    val biggestExpenseTitle: String = "",
    val topCategoryName: String = "",
    val topCategoryAmount: Long = 0L,
    val highestSpendDay: HighestSpendDay? = null,
    /** Expected total month expense (avg daily × days in month); only for THIS_MONTH. */
    val projectedMonthExpense: Long? = null
)

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val selectedRange: AnalyticsRange = AnalyticsRange.THIS_MONTH,
    val selectedGranularity: AnalyticsGranularity = AnalyticsGranularity.DAILY,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netSavings: Long = 0L,
    val comparison: TrendComparison? = null,
    val dailyPoints: List<FlowPoint> = emptyList(),
    val monthlyPoints: List<FlowPoint> = emptyList(),
    val yearlyPoints: List<FlowPoint> = emptyList(),
    val averages: AnalyticsAverages = AnalyticsAverages(0L, 0L, 0L, null),
    val transactionsCount: Int = 0,
    val categoryBreakdown: List<CategorySpendData> = emptyList(),
    val insights: AnalyticsInsights = AnalyticsInsights(),
    val currency: AppCurrency = AppCurrency.USD,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en"
) {
    /** Trend points matching the currently selected granularity. */
    val trendPoints: List<FlowPoint>
        get() = when (selectedGranularity) {
            AnalyticsGranularity.DAILY -> dailyPoints
            AnalyticsGranularity.MONTHLY -> monthlyPoints
            AnalyticsGranularity.YEARLY -> yearlyPoints
        }
}
