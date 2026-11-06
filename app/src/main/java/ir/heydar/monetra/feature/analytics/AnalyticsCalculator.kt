package ir.heydar.monetra.feature.analytics

import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.calendar.PersianDigits
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import saman.zamani.persiandate.PersianDate
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure (UI/Android-free) aggregation logic behind the reports page. Every function takes `now`
 * explicitly so results are deterministic and unit-testable. All month/year windows are computed
 * with [AppDateFormatter] so the Jalali calendar is respected everywhere.
 */
object AnalyticsCalculator {

    private const val DAY_MS = 86_400_000L
    private const val WEEK_MS = 7 * DAY_MS

    /** Above this many daily buckets the chart switches to weekly aggregation. */
    private const val MAX_DAILY_POINTS = 150

    /** Persian transliterations of the Jalali month names for non-fa locales. */
    private val JALALI_MONTHS_EN = arrayOf(
        "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"
    )

    private val UNCATEGORIZED_COLOR = "#6B7280"

    // ---------------------------------------------------------------- ranges

    /** Inclusive start of the selected range (epoch millis). */
    fun rangeStart(range: AnalyticsRange, now: Long, calendar: CalendarSystem): Long = when (range) {
        AnalyticsRange.THIS_MONTH -> AppDateFormatter.monthRange(now, calendar).first
        // Current month + the two before it: walk back two month boundaries.
        AnalyticsRange.LAST_3_MONTHS -> {
            var anchor = now
            repeat(2) { anchor = AppDateFormatter.monthRange(anchor, calendar).first - 1 }
            AppDateFormatter.monthRange(anchor, calendar).first
        }
        AnalyticsRange.THIS_YEAR -> AppDateFormatter.yearRange(now, calendar).first
        AnalyticsRange.ALL_TIME -> 0L
    }

    /**
     * The equivalent window immediately before the selected range, aligned to the elapsed
     * duration of the current range so a partial month is compared with the same slice of the
     * previous one. Null when no meaningful previous window exists.
     */
    fun previousWindow(range: AnalyticsRange, now: Long, calendar: CalendarSystem): LongRange? {
        if (range == AnalyticsRange.ALL_TIME) return null
        val start = rangeStart(range, now, calendar)
        val elapsed = (now - start).coerceAtLeast(0L)
        return when (range) {
            AnalyticsRange.THIS_MONTH -> {
                val prev = AppDateFormatter.monthRange(start - 1, calendar)
                prev.first..minOf(prev.last, prev.first + elapsed)
            }
            AnalyticsRange.LAST_3_MONTHS -> {
                // start-1 is the last instant of the month right before the window.
                val prev = AppDateFormatter.monthRange(start - 1, calendar)
                prev.first..(prev.first + elapsed)
            }
            AnalyticsRange.THIS_YEAR -> {
                val prev = AppDateFormatter.yearRange(start - 1, calendar)
                prev.first..minOf(prev.last, prev.first + elapsed)
            }
            AnalyticsRange.ALL_TIME -> null
        }
    }

    // ---------------------------------------------------------------- trend points

    /**
     * Buckets the transactions per day across `[startMillis, endMillis]` (capped at [now]).
     * Day boundaries follow the local zone so DST shifts never skip or duplicate a bucket.
     * Beyond [MAX_DAILY_POINTS] days the points are merged into weekly buckets.
     */
    fun buildDailyPoints(
        transactions: List<Transaction>,
        startMillis: Long,
        endMillis: Long,
        now: Long,
        calendar: CalendarSystem,
        locale: Locale,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<FlowPoint> {
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val lastDate = minOf(
            Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        )
        if (lastDate.isBefore(startDate)) return emptyList()

        data class Bucket(var income: Long = 0L, var expense: Long = 0L)

        val buckets = HashMap<LocalDate, Bucket>()
        transactions.forEach { tx ->
            if (tx.timestamp in startMillis..endMillis && tx.type != TransactionType.TRANSFER) {
                val day = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
                val bucket = buckets.getOrPut(day) { Bucket() }
                when (tx.type) {
                    TransactionType.INCOME -> bucket.income += tx.amountMinorUnits
                    TransactionType.EXPENSE -> bucket.expense += tx.amountMinorUnits
                    TransactionType.TRANSFER -> Unit
                }
            }
        }

        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, lastDate).toInt() + 1
        val weekly = days > MAX_DAILY_POINTS
        val points = mutableListOf<FlowPoint>()

        var index = 0
        while (index < days) {
            val chunkDays = if (weekly) minOf(7, days - index) else 1
            val chunkStart = startDate.plusDays(index.toLong())
            var income = 0L
            var expense = 0L
            repeat(chunkDays) { offset ->
                buckets[chunkStart.plusDays(offset.toLong())]?.let {
                    income += it.income
                    expense += it.expense
                }
            }
            val chunkEnd = chunkStart.plusDays(chunkDays.toLong() - 1)
            points += FlowPoint(
                label = localizeNumber(chunkStart.dayOfMonth.toString(), locale),
                fullLabel = formatShortDate(chunkEnd.toEpochMilli(zone), calendar, locale, zone),
                income = income,
                expense = expense
            )
            index += chunkDays
        }
        return points
    }

    /** Income/expense totals for the trailing [monthsCount] months ending with the current one. */
    fun buildMonthlyPoints(
        transactions: List<Transaction>,
        now: Long,
        calendar: CalendarSystem,
        locale: Locale,
        monthsCount: Int
    ): List<FlowPoint> {
        val ranges = ArrayDeque<LongRange>()
        var anchor = now
        repeat(monthsCount.coerceAtLeast(1)) {
            val range = AppDateFormatter.monthRange(anchor, calendar)
            ranges.addFirst(range)
            anchor = range.first - 1
        }
        return ranges.map { range ->
            val txs = transactions.filter { it.timestamp in range }
            FlowPoint(
                label = monthLabel(range.first, calendar, locale),
                fullLabel = monthFullLabel(range.first, calendar, locale),
                income = sumByType(txs, TransactionType.INCOME),
                expense = sumByType(txs, TransactionType.EXPENSE)
            )
        }
    }

    /** Income/expense totals for the trailing [yearsCount] years ending with the current one. */
    fun buildYearlyPoints(
        transactions: List<Transaction>,
        now: Long,
        calendar: CalendarSystem,
        locale: Locale,
        yearsCount: Int
    ): List<FlowPoint> {
        val ranges = ArrayDeque<LongRange>()
        var anchor = now
        repeat(yearsCount.coerceAtLeast(1)) {
            val range = AppDateFormatter.yearRange(anchor, calendar)
            ranges.addFirst(range)
            anchor = range.first - 1
        }
        return ranges.map { range ->
            val txs = transactions.filter { it.timestamp in range }
            val year = yearOf(range.first, calendar)
            FlowPoint(
                label = localizeNumber(year.toString(), locale),
                fullLabel = localizeNumber(year.toString(), locale),
                income = sumByType(txs, TransactionType.INCOME),
                expense = sumByType(txs, TransactionType.EXPENSE)
            )
        }
    }

    // ---------------------------------------------------------------- aggregates

    /** Income, expense and transaction count for the window `[startMillis, min(endMillis, now)]`. */
    fun computeRangeTotals(
        transactions: List<Transaction>,
        startMillis: Long,
        endMillis: Long,
        now: Long
    ): RangeTotals {
        val current = transactions.filter { it.timestamp >= startMillis && it.timestamp <= minOf(endMillis, now) }
        return RangeTotals(
            income = sumByType(current, TransactionType.INCOME),
            expense = sumByType(current, TransactionType.EXPENSE),
            count = current.size
        )
    }

    fun computeComparison(
        transactions: List<Transaction>,
        range: AnalyticsRange,
        now: Long,
        calendar: CalendarSystem
    ): TrendComparison? {
        val window = previousWindow(range, now, calendar) ?: return null
        val prevTxs = transactions.filter { it.timestamp in window }
        val prevIncome = sumByType(prevTxs, TransactionType.INCOME)
        val prevExpense = sumByType(prevTxs, TransactionType.EXPENSE)

        val current = transactions.filter { it.timestamp >= rangeStart(range, now, calendar) }
        val income = sumByType(current, TransactionType.INCOME)
        val expense = sumByType(current, TransactionType.EXPENSE)

        return TrendComparison(
            incomeChangePct = changePct(income, prevIncome),
            expenseChangePct = changePct(expense, prevExpense)
        )
    }

    fun computeAverages(
        transactions: List<Transaction>,
        range: AnalyticsRange,
        now: Long,
        calendar: CalendarSystem,
        monthlyPoints: List<FlowPoint>
    ): AnalyticsAverages {
        val start = rangeStart(range, now, calendar)
        val elapsedDays = (((now - start) / DAY_MS) + 1).toInt().coerceAtLeast(1)

        val current = transactions.filter { it.timestamp >= start && it.timestamp <= now }
        val income = sumByType(current, TransactionType.INCOME)
        val expense = sumByType(current, TransactionType.EXPENSE)

        val monthlyCount = monthlyPoints.size.coerceAtLeast(1)
        return AnalyticsAverages(
            dailyExpense = expense / elapsedDays,
            monthlyIncome = monthlyPoints.sumOf { it.income } / monthlyCount,
            monthlyExpense = monthlyPoints.sumOf { it.expense } / monthlyCount,
            savingsRate = if (income > 0L) (income - expense).toFloat() / income else null
        )
    }

    fun computeInsights(
        transactions: List<Transaction>,
        range: AnalyticsRange,
        now: Long,
        calendar: CalendarSystem,
        dailyPoints: List<FlowPoint>,
        categoryBreakdown: List<CategorySpendData>,
        categories: List<Category>,
        averages: AnalyticsAverages
    ): AnalyticsInsights {
        val start = rangeStart(range, now, calendar)
        val expenses = transactions.filter {
            it.timestamp >= start && it.timestamp <= now && it.type == TransactionType.EXPENSE
        }
        val biggest = expenses.maxByOrNull { it.amountMinorUnits }
        val categoryMap = categories.associateBy { it.id }
        val biggestTitle = biggest?.description?.takeIf { it.isNotBlank() }
            ?: biggest?.categoryId?.let { categoryMap[it]?.name }
            ?: ""

        val highestSpendDay = dailyPoints
            .maxByOrNull { it.expense }
            ?.takeIf { it.expense > 0L }
            ?.let { HighestSpendDay(label = it.fullLabel, amount = it.expense) }

        val projected = if (range == AnalyticsRange.THIS_MONTH && averages.dailyExpense > 0L) {
            val daysInMonth = dayCount(AppDateFormatter.monthRange(now, calendar))
            averages.dailyExpense * daysInMonth
        } else {
            null
        }

        return AnalyticsInsights(
            biggestExpenseAmount = biggest?.amountMinorUnits ?: 0L,
            biggestExpenseTitle = biggestTitle,
            topCategoryName = categoryBreakdown.firstOrNull()?.categoryName.orEmpty(),
            topCategoryAmount = categoryBreakdown.firstOrNull()?.amount ?: 0L,
            highestSpendDay = highestSpendDay,
            projectedMonthExpense = projected
        )
    }

    /** Expense totals grouped by category, sorted descending, with share of total expense. */
    fun buildCategoryBreakdown(
        transactions: List<Transaction>,
        categories: List<Category>,
        startMillis: Long,
        endMillis: Long,
        now: Long
    ): List<CategorySpendData> {
        val categoryMap = categories.associateBy { it.id }
        val expenseGroups = transactions
            .filter {
                it.timestamp in startMillis..minOf(endMillis, now) &&
                    it.type == TransactionType.EXPENSE
            }
            .groupBy { it.categoryId }
        val total = expenseGroups.values.sumOf { group -> group.sumOf { it.amountMinorUnits } }
        if (total <= 0L) return emptyList()

        return expenseGroups.map { (categoryId, txs) ->
            val category = categoryId?.let { categoryMap[it] }
            val amount = txs.sumOf { it.amountMinorUnits }
            CategorySpendData(
                categoryId = categoryId,
                categoryName = category?.name ?: "",
                colorHex = category?.color?.takeIf { it.isNotBlank() } ?: UNCATEGORIZED_COLOR,
                amount = amount,
                percentage = amount.toFloat() / total,
                transactionCount = txs.size
            )
        }.sortedByDescending { it.amount }
    }

    // ---------------------------------------------------------------- helpers

    /** Raw totals of the selected range. */
    data class RangeTotals(val income: Long, val expense: Long, val count: Int)

    private fun sumByType(transactions: List<Transaction>, type: TransactionType): Long =
        transactions.filter { it.type == type }.sumOf { it.amountMinorUnits }

    /** Percentage change from [previous] to [current], null when there is no baseline. */
    private fun changePct(current: Long, previous: Long): Float? {
        if (previous <= 0L) return null
        return ((current - previous).toFloat() / previous) * 100f
    }

    private fun dayCount(range: LongRange): Int =
        (((range.last - range.first) / DAY_MS) + 1).toInt().coerceAtLeast(1)

    private fun monthLabel(timestamp: Long, calendar: CalendarSystem, locale: Locale): String =
        when (calendar) {
            CalendarSystem.GREGORIAN ->
                SimpleDateFormat("MMM", locale).format(timestamp)
            CalendarSystem.JALALI ->
                if (locale.language == "fa") {
                    PersianDate(timestamp).monthName()
                } else {
                    JALALI_MONTHS_EN[PersianDate(timestamp).shMonth - 1]
                }
        }

    private fun monthFullLabel(timestamp: Long, calendar: CalendarSystem, locale: Locale): String =
        when (calendar) {
            CalendarSystem.GREGORIAN ->
                SimpleDateFormat("MMM yyyy", locale).format(timestamp)
            CalendarSystem.JALALI -> {
                val date = PersianDate(timestamp)
                val year = localizeNumber(date.shYear.toString(), locale)
                "${monthLabel(timestamp, calendar, locale)} $year"
            }
        }

    private fun formatShortDate(
        timestamp: Long,
        calendar: CalendarSystem,
        locale: Locale,
        zone: ZoneId
    ): String = when (calendar) {
        CalendarSystem.GREGORIAN -> SimpleDateFormat("d MMM", locale).format(timestamp)
        CalendarSystem.JALALI -> {
            val date = PersianDate(timestamp)
            val day = localizeNumber(date.shDay.toString(), locale)
            val month = if (locale.language == "fa") {
                date.monthName()
            } else {
                JALALI_MONTHS_EN[date.shMonth - 1]
            }
            "$day $month"
        }
    }

    private fun yearOf(timestamp: Long, calendar: CalendarSystem): Int = when (calendar) {
        CalendarSystem.GREGORIAN ->
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).year
        CalendarSystem.JALALI -> PersianDate(timestamp).shYear
    }

    private fun localizeNumber(text: String, locale: Locale): String =
        if (locale.language == "fa") PersianDigits.toPersian(text) else text
}

private fun LocalDate.toEpochMilli(zone: ZoneId): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()
