package ir.heydar.monetra.feature.analytics

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class AnalyticsCalculatorTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millis(date: String, hour: Int = 12): Long {
        val localDate = LocalDate.parse(date).atTime(hour, 0)
        return localDate.atZone(zone).toInstant().toEpochMilli()
    }

    private fun tx(
        timestamp: Long,
        amount: Long,
        type: TransactionType,
        categoryId: Long? = null,
        description: String? = null
    ) = Transaction(
        accountId = 1L,
        amountMinorUnits = amount,
        currencyCode = "TOMAN",
        type = type,
        categoryId = categoryId,
        description = description,
        timestamp = timestamp
    )

    // ---------------------------------------------------------------- yearRange

    @Test
    fun `jalali yearRange covers full 1403 year`() {
        val now = millis("2024-06-15") // Mordad 1403
        val range = ir.heydar.monetra.core.calendar.AppDateFormatter.yearRange(now, CalendarSystem.JALALI)

        // 1403 Nowruz is 2024-03-20, 1404 Nowruz is 2025-03-21.
        assertEquals(millis("2024-03-20", 0), range.first)
        // Last inclusive millisecond of the year: end of 2025-03-20.
        assertEquals(millis("2025-03-20", 23) + 59 * 60 * 1000 + 59_999, range.last)
    }

    @Test
    fun `gregorian yearRange covers calendar year`() {
        val now = millis("2024-06-15")
        val range = ir.heydar.monetra.core.calendar.AppDateFormatter.yearRange(now, CalendarSystem.GREGORIAN)
        assertEquals(millis("2024-01-01", 0), range.first)
        assertEquals(millis("2024-12-31", 23) + 59 * 60 * 1000 + 59_999, range.last)
    }

    // ---------------------------------------------------------------- monthly points

    @Test
    fun `monthly buckets respect jalali month boundaries`() {
        // Esfand 29, 1403 (2025-03-20) belongs to the previous bucket,
        // Farvardin 1, 1404 (2025-03-21) to the current one.
        val endOfMonthBeforeNowruz = millis("2025-03-20")
        val firstDayOfNowruz = millis("2025-03-21")
        val now = millis("2025-04-10")

        val transactions = listOf(
            tx(endOfMonthBeforeNowruz, 100L, TransactionType.EXPENSE),
            tx(firstDayOfNowruz, 200L, TransactionType.EXPENSE)
        )

        val points = AnalyticsCalculator.buildMonthlyPoints(
            transactions = transactions,
            now = now,
            calendar = CalendarSystem.JALALI,
            locale = Locale("fa"),
            monthsCount = 2
        )

        assertEquals(2, points.size)
        // The oldest bucket (Esfand 1403) only holds the pre-Nowruz transaction.
        assertEquals(100L, points[0].expense)
        assertEquals(200L, points[1].expense)
    }

    @Test
    fun `monthly buckets respect gregorian month boundaries`() {
        val endOfMarch = millis("2025-03-31")
        val firstOfApril = millis("2025-04-01")
        val now = millis("2025-04-15")

        val transactions = listOf(
            tx(endOfMarch, 100L, TransactionType.EXPENSE),
            tx(firstOfApril, 300L, TransactionType.EXPENSE)
        )

        val points = AnalyticsCalculator.buildMonthlyPoints(
            transactions, now, CalendarSystem.GREGORIAN, Locale.ENGLISH, monthsCount = 2
        )

        assertEquals(2, points.size)
        assertEquals(100L, points[0].expense)
        assertEquals(300L, points[1].expense)
    }

    @Test
    fun `monthly points exclude transfers from totals`() {
        val now = millis("2025-04-15")
        val transactions = listOf(
            tx(millis("2025-04-05"), 500L, TransactionType.INCOME),
            tx(millis("2025-04-06"), 700L, TransactionType.TRANSFER)
        )
        val points = AnalyticsCalculator.buildMonthlyPoints(
            transactions, now, CalendarSystem.GREGORIAN, Locale.ENGLISH, monthsCount = 3
        )
        assertEquals(500L, points.last().income)
        assertEquals(0L, points.last().expense)
    }

    // ---------------------------------------------------------------- yearly points

    @Test
    fun `yearly points aggregate per year`() {
        val now = millis("2025-04-15")
        val transactions = listOf(
            tx(millis("2023-05-01"), 1000L, TransactionType.INCOME),
            tx(millis("2024-02-01"), 2000L, TransactionType.EXPENSE),
            tx(millis("2025-03-01"), 3000L, TransactionType.EXPENSE)
        )
        val points = AnalyticsCalculator.buildYearlyPoints(
            transactions, now, CalendarSystem.GREGORIAN, Locale.ENGLISH, yearsCount = 3
        )
        assertEquals(3, points.size)
        assertEquals(1000L, points[0].income)
        assertEquals(2000L, points[1].expense)
        assertEquals(3000L, points[2].expense)
    }

    // ---------------------------------------------------------------- daily points

    @Test
    fun `daily buckets split by local day`() {
        val now = millis("2025-04-03", 23)
        val transactions = listOf(
            tx(millis("2025-04-01", 9), 100L, TransactionType.EXPENSE),
            tx(millis("2025-04-01", 22), 150L, TransactionType.EXPENSE),
            tx(millis("2025-04-02", 9), 50L, TransactionType.INCOME)
        )
        val points = AnalyticsCalculator.buildDailyPoints(
            transactions, millis("2025-04-01", 0), now, now,
            CalendarSystem.GREGORIAN, Locale.ENGLISH, zone
        )
        assertEquals(3, points.size)
        assertEquals(250L, points[0].expense)
        assertEquals(50L, points[1].income)
        assertEquals(0L, points[2].income)
        assertEquals(0L, points[2].expense)
    }

    @Test
    fun `daily points switch to weekly buckets beyond 150 days`() {
        val now = millis("2025-04-01", 12)
        val start = millis("2024-01-01", 0) // ~456 days
        val transactions = listOf(tx(millis("2024-01-03", 10), 700L, TransactionType.EXPENSE))

        val points = AnalyticsCalculator.buildDailyPoints(
            transactions, start, now, now,
            CalendarSystem.GREGORIAN, Locale.ENGLISH, zone
        )

        // 456 days / 7-day buckets => ~66 points, all far fewer than 150.
        assertTrue(points.size < 150)
        // First weekly bucket contains the 2024-01-03 expense.
        assertEquals(700L, points.first().expense)
    }

    // ---------------------------------------------------------------- comparison

    @Test
    fun `comparison aligns with elapsed slice of previous month`() {
        // "Now" is April 10th, so ~9 days elapsed; the previous window must stay inside March.
        val now = millis("2025-04-10", 12)
        val transactions = listOf(
            // Current month (April 1..10): 100 expense
            tx(millis("2025-04-05"), 100L, TransactionType.EXPENSE),
            // Same slice of March (March 1..10): 80 expense
            tx(millis("2025-03-05"), 80L, TransactionType.EXPENSE),
            // Late March (March 25) must be ignored
            tx(millis("2025-03-25"), 5000L, TransactionType.EXPENSE)
        )

        val comparison = AnalyticsCalculator.computeComparison(
            transactions, AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN
        )

        val change = comparison?.expenseChangePct
        assertEquals(((100f - 80f) / 80f * 100f), change!!, 0.01f)
    }

    @Test
    fun `comparison is null for all time and without baseline`() {
        val now = millis("2025-04-10", 12)
        assertNull(
            AnalyticsCalculator.computeComparison(
                emptyList(), AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN
            )?.expenseChangePct
        )
        assertNull(
            AnalyticsCalculator.computeComparison(
                listOf(tx(millis("2025-04-01"), 10L, TransactionType.EXPENSE)),
                AnalyticsRange.ALL_TIME, now, CalendarSystem.GREGORIAN
            )
        )
    }

    // ---------------------------------------------------------------- averages

    @Test
    fun `averages compute daily expense and savings rate`() {
        val now = millis("2025-04-10", 12) // day 10 => 10 elapsed days
        val transactions = listOf(
            tx(millis("2025-04-02"), 300L, TransactionType.EXPENSE),
            tx(millis("2025-04-05"), 100L, TransactionType.EXPENSE),
            tx(millis("2025-04-03"), 1000L, TransactionType.INCOME)
        )
        val monthly = AnalyticsCalculator.buildMonthlyPoints(
            transactions, now, CalendarSystem.GREGORIAN, Locale.ENGLISH, monthsCount = 2
        )
        val averages = AnalyticsCalculator.computeAverages(
            transactions, AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN, monthly
        )

        assertEquals(400L / 10L, averages.dailyExpense)
        // Two monthly buckets (March empty + April): averages are divided by bucket count.
        assertEquals(1000L / 2L, averages.monthlyIncome)
        assertEquals(400L / 2L, averages.monthlyExpense)
        assertEquals((1000L - 400L) / 1000f, averages.savingsRate!!, 0.001f)
    }

    @Test
    fun `savings rate is null without income`() {
        val now = millis("2025-04-10", 12)
        val transactions = listOf(tx(millis("2025-04-02"), 300L, TransactionType.EXPENSE))
        val averages = AnalyticsCalculator.computeAverages(
            transactions, AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN, emptyList()
        )
        assertNull(averages.savingsRate)
    }

    // ---------------------------------------------------------------- insights

    @Test
    fun `insights find biggest expense and highest spend day`() {
        val now = millis("2025-04-10", 12)
        val food = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE, color = "#10B981")
        val transactions = listOf(
            tx(millis("2025-04-02"), 200L, TransactionType.EXPENSE, categoryId = 1L),
            tx(millis("2025-04-05"), 900L, TransactionType.EXPENSE, categoryId = 1L, description = "Phone"),
            tx(millis("2025-04-05"), 300L, TransactionType.EXPENSE, categoryId = 1L),
            tx(millis("2025-04-06"), 50L, TransactionType.INCOME)
        )
        val breakdown = AnalyticsCalculator.buildCategoryBreakdown(
            transactions, listOf(food), millis("2025-04-01", 0), now, now
        )
        val daily = AnalyticsCalculator.buildDailyPoints(
            transactions, millis("2025-04-01", 0), now, now,
            CalendarSystem.GREGORIAN, Locale.ENGLISH, zone
        )
        val averages = AnalyticsCalculator.computeAverages(
            transactions, AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN, emptyList()
        )
        val insights = AnalyticsCalculator.computeInsights(
            transactions, AnalyticsRange.THIS_MONTH, now, CalendarSystem.GREGORIAN,
            daily, breakdown, listOf(food), averages
        )

        assertEquals(900L, insights.biggestExpenseAmount)
        assertEquals("Phone", insights.biggestExpenseTitle)
        assertEquals("Food", insights.topCategoryName)
        assertEquals(1400L, insights.topCategoryAmount)
        assertEquals(1200L, insights.highestSpendDay?.amount)
        // Projection: avg daily (1400 / 10 elapsed days) * 30 days of April.
        assertEquals((1400L / 10L) * 30L, insights.projectedMonthExpense)
    }

    @Test
    fun `projection is null outside this month`() {
        val now = millis("2025-04-10", 12)
        val transactions = listOf(tx(millis("2025-04-02"), 200L, TransactionType.EXPENSE))
        val insights = AnalyticsCalculator.computeInsights(
            transactions, AnalyticsRange.LAST_3_MONTHS, now, CalendarSystem.GREGORIAN,
            emptyList(), emptyList(), emptyList(),
            AnalyticsAverages(20L, 0L, 0L, null)
        )
        assertNull(insights.projectedMonthExpense)
    }

    // ---------------------------------------------------------------- categories

    @Test
    fun `category breakdown sorts by amount and computes shares`() {
        val now = millis("2025-04-10", 12)
        val food = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE, color = "#10B981")
        val transport = Category(id = 2L, name = "Transport", type = TransactionType.EXPENSE, color = "#3B82F6")
        val transactions = listOf(
            tx(millis("2025-04-02"), 300L, TransactionType.EXPENSE, categoryId = 1L),
            tx(millis("2025-04-03"), 100L, TransactionType.EXPENSE, categoryId = 1L),
            tx(millis("2025-04-04"), 600L, TransactionType.EXPENSE, categoryId = 2L),
            tx(millis("2025-04-05"), 400L, TransactionType.INCOME, categoryId = 1L),
            tx(millis("2025-04-06"), 200L, TransactionType.EXPENSE, categoryId = null)
        )

        val breakdown = AnalyticsCalculator.buildCategoryBreakdown(
            transactions, listOf(food, transport), millis("2025-04-01", 0), now, now
        )

        assertEquals(3, breakdown.size)
        assertEquals("Transport", breakdown[0].categoryName)
        assertEquals(600L, breakdown[0].amount)
        assertEquals(0.5f, breakdown[0].percentage, 0.001f)
        assertEquals("Food", breakdown[1].categoryName)
        assertEquals(400L, breakdown[1].amount)
        assertEquals(1f / 3f, breakdown[1].percentage, 0.001f)
        // Uncategorized keeps the neutral gray.
        assertEquals("#6B7280", breakdown[2].colorHex)
    }

    // ---------------------------------------------------------------- range start

    @Test
    fun `range start for last 3 months walks jalali months correctly`() {
        val now = millis("2025-04-15") // Farvardin 1404
        val start = AnalyticsCalculator.rangeStart(
            AnalyticsRange.LAST_3_MONTHS, now, CalendarSystem.JALALI
        )
        // Window = Bahman + Esfand + Farvardin, so it starts on Bahman 1, 1403 = 2025-01-20.
        assertEquals(millis("2025-01-20", 0), start)
    }
}
