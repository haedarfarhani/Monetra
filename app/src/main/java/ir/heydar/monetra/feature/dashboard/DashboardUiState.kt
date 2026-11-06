package ir.heydar.monetra.feature.dashboard

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Transaction

/** One month of the compact dashboard overview chart. Amounts use the app's minor-unit storage. */
data class MonthlyTrendPoint(
    val label: String,
    val income: Long,
    val expense: Long
)

/** One spending category aggregated over the current month. */
data class CategorySpending(
    val categoryId: Long?,
    val name: String,
    val amount: Long,
    /** Share of the total monthly expense, 0f..1f. */
    val percentage: Float
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val totalBalance: Long = 0L,
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyTrend: List<MonthlyTrendPoint> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val topCategories: List<CategorySpending> = emptyList(),
    val daysInMonth: Int = 30,
    val dayOfMonth: Int = 1,
    val isPrivacyEnabled: Boolean = false,
    val currencyCode: String = "USD",
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en",
    val error: String? = null
) {
    /** Share of income kept this month, 0f..1f (0 when there is no income). */
    val savingsRate: Float
        get() = if (monthlyIncome > 0L) {
            ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome).coerceIn(0f, 1f)
        } else {
            0f
        }
}
