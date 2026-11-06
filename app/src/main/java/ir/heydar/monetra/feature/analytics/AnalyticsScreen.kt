package ir.heydar.monetra.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.PersianDigits
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.charts.ChartReferenceLine
import ir.heydar.monetra.core.designsystem.charts.ChartSeries
import ir.heydar.monetra.core.designsystem.charts.DonutSlice
import ir.heydar.monetra.core.designsystem.charts.MonetraBarChart
import ir.heydar.monetra.core.designsystem.charts.MonetraDonutChart
import ir.heydar.monetra.core.designsystem.charts.MonetraLineChart
import ir.heydar.monetra.core.designsystem.components.AppCard
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.components.MonetraCategoryIcon
import ir.heydar.monetra.core.designsystem.components.MonetraSectionHeader
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.TransactionType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnalyticsRoute(
    onBack: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsScreen(
        uiState = uiState,
        onSelectRange = viewModel::selectRange,
        onSelectGranularity = viewModel::selectGranularity,
        onBack = onBack
    )
}

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onSelectRange: (AnalyticsRange) -> Unit = {},
    onSelectGranularity: (AnalyticsGranularity) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val locale = remember(uiState.language) {
        if (uiState.language.isBlank()) Locale.getDefault() else Locale.forLanguageTag(uiState.language)
    }
    val extendedColors = MaterialTheme.monetraColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.analytics_title),
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val hasAnyData = uiState.transactionsCount > 0 ||
            uiState.totalIncome > 0L || uiState.totalExpense > 0L
        if (!hasAnyData) {
            EmptyState(
                title = stringResource(id = R.string.analytics_no_data_title),
                message = stringResource(id = R.string.analytics_no_data_message),
                icon = Icons.Outlined.Insights,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // 1) Range selector chips
            item(key = "range") {
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.entries.forEachIndexed { index, range ->
                        GlassFilterChip(
                            selected = uiState.selectedRange == range,
                            onClick = { onSelectRange(range) },
                            label = {
                                Text(
                                    text = rangeLabelComposable(range),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2) Period summary: totals, net savings and comparison with previous period
            item(key = "summary") {
                PeriodSummaryCard(uiState = uiState, locale = locale)
            }

            // 3) Trend chart with daily / monthly / yearly granularity
            item(key = "trend") {
                TrendCard(
                    uiState = uiState,
                    locale = locale,
                    onSelectGranularity = onSelectGranularity
                )
            }

            // 4) Averages & stats grid
            item(key = "averages") {
                MonetraSectionHeader(title = stringResource(id = R.string.analytics_averages_title))
                AveragesCard(uiState = uiState, locale = locale)
            }

            // 5) Category breakdown: donut chart + per-category rows
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item(key = "category_header") {
                    MonetraSectionHeader(title = stringResource(id = R.string.analytics_by_category))
                }
                item(key = "category_donut") {
                    CategoryDonutCard(uiState = uiState, locale = locale)
                }
                items(
                    items = uiState.categoryBreakdown,
                    key = { "category_${it.categoryId ?: -1}" }
                ) { category ->
                    CategoryProgressRow(
                        data = category,
                        currency = uiState.currency,
                        locale = locale
                    )
                }
            }

            // 6) Insights
            if (uiState.insights.biggestExpenseAmount > 0L ||
                uiState.insights.highestSpendDay != null ||
                uiState.insights.projectedMonthExpense != null
            ) {
                item(key = "insights_header") {
                    MonetraSectionHeader(title = stringResource(id = R.string.analytics_insights_title))
                }
                item(key = "insights_grid") {
                    InsightsGrid(uiState = uiState, locale = locale)
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 1: Period summary
// ---------------------------------------------------------------------------

@Composable
private fun PeriodSummaryCard(uiState: AnalyticsUiState, locale: Locale) {
    val extendedColors = MaterialTheme.monetraColors
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.analytics_period_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val savingsColor = if (uiState.netSavings >= 0) extendedColors.income else extendedColors.expense
                Surface(
                    shape = CircleShape,
                    color = savingsColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = (if (uiState.netSavings >= 0) "+" else "") +
                            MoneyFormatter.format(uiState.netSavings, uiState.currency, locale),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = savingsColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.analytics_income),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(uiState.totalIncome, uiState.currency, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.income
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(id = R.string.analytics_expense),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(uiState.totalExpense, uiState.currency, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.expense
                    )
                }
            }

            // Segmented income/expense ratio bar
            val total = (uiState.totalIncome + uiState.totalExpense).coerceAtLeast(1L)
            val incomeRatio = (uiState.totalIncome.toFloat() / total).coerceIn(0f, 1f)
            val expenseRatio = (uiState.totalExpense.toFloat() / total).coerceIn(0f, 1f)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            ) {
                if (incomeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(incomeRatio)
                            .fillMaxSize()
                            .background(extendedColors.income)
                    )
                }
                if (expenseRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(expenseRatio)
                            .fillMaxSize()
                            .background(extendedColors.expense)
                    )
                }
            }

            // Comparison with the previous period
            uiState.comparison?.let { comparison ->
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Text(
                    text = stringResource(id = R.string.analytics_vs_previous),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    ComparisonStat(
                        label = stringResource(id = R.string.analytics_income),
                        changePct = comparison.incomeChangePct,
                        higherIsGood = true,
                        locale = locale,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonStat(
                        label = stringResource(id = R.string.analytics_expense),
                        changePct = comparison.expenseChangePct,
                        higherIsGood = false,
                        locale = locale,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonStat(
    label: String,
    changePct: Float?,
    higherIsGood: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    val extendedColors = MaterialTheme.monetraColors
    val hasChange = changePct != null
    // For income up is good; for expense down is good.
    val isGood = changePct?.let { if (higherIsGood) it >= 0f else it <= 0f } ?: true
    val tint = when {
        !hasChange -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> extendedColors.income
        else -> extendedColors.expense
    }
    val containerColor = MaterialTheme.glassColors.innerFillTop

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (changePct != null) {
            Icon(
                imageVector = if (changePct >= 0f) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = formatChangePct(changePct, locale),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Section 2: Trend chart
// ---------------------------------------------------------------------------

@Composable
private fun TrendCard(
    uiState: AnalyticsUiState,
    locale: Locale,
    onSelectGranularity: (AnalyticsGranularity) -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            Text(
                text = stringResource(id = R.string.analytics_trend_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            GranularitySegmented(
                selected = uiState.selectedGranularity,
                onSelect = onSelectGranularity
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = extendedColors.income,
                    label = stringResource(id = R.string.analytics_income)
                )
                ChartLegendItem(
                    color = extendedColors.expense,
                    label = stringResource(id = R.string.analytics_expense)
                )
            }

            val points = uiState.trendPoints
            if (points.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.analytics_empty_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                val incomeSeries = ChartSeries(
                    title = stringResource(id = R.string.analytics_income),
                    values = points.map { it.income.toFloat() },
                    color = extendedColors.income
                )
                val expenseSeries = ChartSeries(
                    title = stringResource(id = R.string.analytics_expense),
                    values = points.map { it.expense.toFloat() },
                    color = extendedColors.expense
                )
                val formatValue: (Float) -> String = { MoneyFormatter.format(it.toLong(), uiState.currency, locale) }
                val formatAxis: (Float) -> String = { compactMoney(it, uiState.currency, locale) }

                when (uiState.selectedGranularity) {
                    AnalyticsGranularity.DAILY -> MonetraLineChart(
                        labels = points.map { it.label },
                        fullLabels = points.map { it.fullLabel },
                        series = listOf(incomeSeries, expenseSeries),
                        referenceLines = averageReferenceLines(
                            points = points,
                            color = extendedColors.expense,
                            title = stringResource(id = R.string.analytics_average_line)
                        ),
                        formatValue = formatValue,
                        formatAxisValue = formatAxis,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    AnalyticsGranularity.MONTHLY -> MonetraBarChart(
                        labels = points.map { it.label },
                        fullLabels = points.map { it.fullLabel },
                        series = listOf(incomeSeries, expenseSeries),
                        formatValue = formatValue,
                        formatAxisValue = formatAxis,
                        modifier = Modifier.fillMaxWidth().height(210.dp)
                    )
                    AnalyticsGranularity.YEARLY -> MonetraLineChart(
                        labels = points.map { it.label },
                        fullLabels = points.map { it.fullLabel },
                        series = listOf(incomeSeries, expenseSeries),
                        referenceLines = averageReferenceLines(
                            points = points,
                            color = extendedColors.expense,
                            title = stringResource(id = R.string.analytics_average_line)
                        ),
                        formatValue = formatValue,
                        formatAxisValue = formatAxis,
                        modifier = Modifier.fillMaxWidth().height(210.dp)
                    )
                }
            }
        }
    }
}

/** One dashed reference line: the average expense across the visible chart points. */
private fun averageReferenceLines(
    points: List<FlowPoint>,
    color: Color,
    title: String
): List<ChartReferenceLine> {
    if (points.isEmpty()) return emptyList()
    val total = points.sumOf { it.expense }
    if (total <= 0L) return emptyList()
    return listOf(
        ChartReferenceLine(
            title = title,
            value = total.toFloat() / points.size,
            color = color
        )
    )
}

@Composable
private fun GranularitySegmented(
    selected: AnalyticsGranularity,
    onSelect: (AnalyticsGranularity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.glassColors.innerFillTop)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnalyticsGranularity.entries.forEach { granularity ->
            val isSelected = granularity == selected
            val label = when (granularity) {
                AnalyticsGranularity.DAILY -> stringResource(id = R.string.analytics_tab_daily)
                AnalyticsGranularity.MONTHLY -> stringResource(id = R.string.analytics_tab_monthly)
                AnalyticsGranularity.YEARLY -> stringResource(id = R.string.analytics_tab_yearly)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(granularity) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section 3: Averages & stats
// ---------------------------------------------------------------------------

@Composable
private fun AveragesCard(uiState: AnalyticsUiState, locale: Locale) {
    val extendedColors = MaterialTheme.monetraColors
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.analytics_transactions_count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = localizeNumber(uiState.transactionsCount.toString(), locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                StatTile(
                    title = stringResource(id = R.string.analytics_avg_daily_expense),
                    value = MoneyFormatter.format(uiState.averages.dailyExpense, uiState.currency, locale),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = stringResource(id = R.string.analytics_avg_monthly_income),
                    value = MoneyFormatter.format(uiState.averages.monthlyIncome, uiState.currency, locale),
                    valueColor = extendedColors.income,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                StatTile(
                    title = stringResource(id = R.string.analytics_avg_monthly_expense),
                    value = MoneyFormatter.format(uiState.averages.monthlyExpense, uiState.currency, locale),
                    valueColor = extendedColors.expense,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = stringResource(id = R.string.analytics_savings_rate),
                    value = uiState.averages.savingsRate?.let { formatRate(it, locale) }
                        ?: "—",
                    valueColor = extendedColors.success,
                    showRateBar = uiState.averages.savingsRate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showRateBar: Float? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.glassColors.innerFillTop)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        showRateBar?.let { rate ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rate.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.monetraColors.success)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 4: Categories (donut + rows)
// ---------------------------------------------------------------------------

@Composable
private fun CategoryDonutCard(uiState: AnalyticsUiState, locale: Locale) {
    val extendedColors = MaterialTheme.monetraColors
    var selectedSlice by remember(uiState.categoryBreakdown) { mutableIntStateOf(-1) }
    val slices = uiState.categoryBreakdown.map { data ->
        DonutSlice(
            label = data.categoryName,
            value = data.amount.toFloat(),
            color = parseHexColor(data.colorHex)
        )
    }
    val selected = selectedSlice.takeIf { it >= 0 && it < uiState.categoryBreakdown.size }
    val selectedData = selected?.let { uiState.categoryBreakdown[it] }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonetraDonutChart(
                    slices = slices,
                    selectedSlice = selected,
                    onSliceSelected = { slice -> selectedSlice = slice ?: -1 },
                    strokeWidth = 20.dp,
                    modifier = Modifier.size(150.dp),
                    centerContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedData?.categoryName
                                    ?: stringResource(id = R.string.analytics_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Text(
                                text = MoneyFormatter.format(
                                    selectedData?.amount ?: uiState.totalExpense,
                                    uiState.currency,
                                    locale
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.categoryBreakdown.take(4).forEachIndexed { index, data ->
                        val isSelected = selected == index
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedSlice = if (selectedSlice == index) -1 else index
                                }
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isSelected) {
                                            parseHexColor(data.colorHex)
                                        } else {
                                            parseHexColor(data.colorHex).copy(alpha = 0.75f)
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = data.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatPercent(data.percentage, locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selectedData != null) {
                Text(
                    text = "${selectedData.categoryName} — " +
                        MoneyFormatter.format(selectedData.amount, uiState.currency, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = extendedColors.expense,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(
    data: CategorySpendData,
    currency: AppCurrency,
    locale: Locale
) {
    val categoryColor = parseHexColor(data.colorHex)
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonetraCategoryIcon(
                categoryName = data.categoryName,
                transactionType = TransactionType.EXPENSE,
                size = 38.dp
            )
            Spacer(modifier = Modifier.width(Dimens.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatPercent(data.percentage, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = MoneyFormatter.format(data.amount, currency, locale),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.glassColors.innerFillTop)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(data.percentage.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        categoryColor.copy(alpha = 0.75f),
                                        categoryColor
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 5: Insights
// ---------------------------------------------------------------------------

@Composable
private fun InsightsGrid(uiState: AnalyticsUiState, locale: Locale) {
    val insights = uiState.insights
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            if (insights.biggestExpenseAmount > 0L) {
                InsightTile(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(id = R.string.analytics_biggest_expense),
                    value = MoneyFormatter.format(insights.biggestExpenseAmount, uiState.currency, locale),
                    subtitle = insights.biggestExpenseTitle,
                    modifier = Modifier.weight(1f)
                )
            }
            if (insights.topCategoryName.isNotBlank()) {
                InsightTile(
                    icon = Icons.Outlined.Category,
                    title = stringResource(id = R.string.analytics_top_category),
                    value = MoneyFormatter.format(insights.topCategoryAmount, uiState.currency, locale),
                    subtitle = insights.topCategoryName,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
            if (insights.highestSpendDay != null) {
                InsightTile(
                    icon = Icons.Outlined.Event,
                    title = stringResource(id = R.string.analytics_highest_spend_day),
                    value = MoneyFormatter.format(insights.highestSpendDay.amount, uiState.currency, locale),
                    subtitle = insights.highestSpendDay.label,
                    modifier = Modifier.weight(1f)
                )
            }
            if (insights.projectedMonthExpense != null) {
                InsightTile(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    title = stringResource(id = R.string.analytics_projection),
                    value = MoneyFormatter.format(insights.projectedMonthExpense, uiState.currency, locale),
                    subtitle = stringResource(id = R.string.analytics_projection_hint),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val extendedColors = MaterialTheme.monetraColors
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extendedColors.expense,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Small shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rangeLabelComposable(range: AnalyticsRange): String = when (range) {
    AnalyticsRange.THIS_MONTH -> stringResource(id = R.string.analytics_range_month)
    AnalyticsRange.LAST_3_MONTHS -> stringResource(id = R.string.analytics_range_3months)
    AnalyticsRange.THIS_YEAR -> stringResource(id = R.string.analytics_range_year)
    AnalyticsRange.ALL_TIME -> stringResource(id = R.string.analytics_range_all)
}

private fun formatChangePct(changePct: Float?, locale: Locale): String {
    if (changePct == null) return "—"
    val sign = if (changePct >= 0f) "+" else "-"
    val text = "$sign${String.format(locale, "%.1f", abs(changePct))}%"
    return localizeNumber(if (locale.language == "fa") text.replace("%", "٪") else text, locale)
}

private fun formatPercent(percentage: Float, locale: Locale): String {
    val text = "${String.format(locale, "%.0f", percentage * 100f)}%"
    return localizeNumber(if (locale.language == "fa") text.replace("%", "٪") else text, locale)
}

private fun formatRate(rate: Float, locale: Locale): String = formatPercent(rate, locale)

/** Compact money text for chart axis labels (K/M scaled, no symbol). */
private fun compactMoney(value: Float, currency: AppCurrency, locale: Locale): String {
    var factor = 1f
    repeat(currency.fractionDigits) { factor *= 10f }
    val whole = value / factor
    val text = when {
        whole >= 1_000_000f -> formatCompact(whole / 1_000_000f) + "M"
        whole >= 1_000f -> formatCompact(whole / 1_000f) + "K"
        else -> formatCompact(whole)
    }
    return localizeNumber(text, locale)
}

private fun formatCompact(value: Float): String =
    if (value >= 10f || value <= -10f) value.roundToInt().toString() else String.format("%.1f", value)

private fun localizeNumber(text: String, locale: Locale): String =
    if (locale.language == "fa") PersianDigits.toPersian(text) else text

/** Parses a stored hex color, falling back to the neutral gray used by seeded categories. */
private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF6B7280)
}
