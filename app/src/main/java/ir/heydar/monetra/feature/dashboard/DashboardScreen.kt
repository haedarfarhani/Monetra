package ir.heydar.monetra.feature.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.PersianDigits
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.EmptyState
import ir.heydar.monetra.core.designsystem.components.ErrorState
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.components.MonetraBalanceCard
import ir.heydar.monetra.core.designsystem.components.MonetraCategoryIcon
import ir.heydar.monetra.core.designsystem.components.MonetraSecondaryButton
import ir.heydar.monetra.core.designsystem.components.MonetraSectionHeader
import ir.heydar.monetra.core.designsystem.components.MonetraTransactionItem
import ir.heydar.monetra.core.designsystem.components.accountTypeIcon
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.core.permissions.PermissionStatus
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.domain.model.TransactionType
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

@Composable
fun DashboardRoute(
    onNavigateToAddTransaction: (TransactionType) -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onEvent(DashboardUiEvent.PermissionsChanged) }

    val missingPermissions = remember(permissionStatus) { viewModel.missingPermissions().toList() }
    // A permission denied for good no longer raises a system dialog, so the only way back is
    // the app's page in system settings. Same when only the notification toggle is off.
    val needsSystemSettings = missingPermissions.isEmpty() || (
        permissionStatus.promptShown &&
            activity != null &&
            missingPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        )

    val onGrantPermissions: () -> Unit = {
        if (needsSystemSettings) {
            viewModel.openAppSettings()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DashboardUiEffect.NavigateToAddTransaction -> onNavigateToAddTransaction(effect.type)
                is DashboardUiEffect.NavigateToTransactionDetail -> onNavigateToTransactionDetail(effect.transactionId)
                is DashboardUiEffect.ShowSnackbar -> { /* handled by screen-level snackbars */ }
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        permissionStatus = permissionStatus,
        needsSystemSettings = needsSystemSettings,
        onGrantPermissions = onGrantPermissions,
        onEvent = viewModel::onEvent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToTransactions = onNavigateToTransactions,
        onNavigateToReports = onNavigateToReports,
        onNavigateToAccounts = onNavigateToAccounts
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    permissionStatus: PermissionStatus = PermissionStatus(),
    needsSystemSettings: Boolean = false,
    onGrantPermissions: () -> Unit = {},
    onEvent: (DashboardUiEvent) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {}
) {
    val locale = remember(uiState.language) { Locale.forLanguageTag(uiState.language) }
    val currency = remember(uiState.currencyCode) { AppCurrency.fromCode(uiState.currencyCode) }

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }

    // Shows the selected calendar (Jalali or Gregorian) right at the top of the dashboard.
    val todayLabel = remember(uiState.calendar, uiState.language) {
        AppDateFormatter.formatDate(System.currentTimeMillis(), uiState.calendar, locale)
    }

    val formatMoney: (Long) -> String = remember(currency, locale) {
        { amount -> MoneyFormatter.format(amount, currency, locale) }
    }
    val mask: (String) -> String = remember(uiState.isPrivacyEnabled) {
        { text -> if (uiState.isPrivacyEnabled) "••••" else text }
    }
    val localizeNumber: (String) -> String = remember(uiState.language) {
        { text -> if (uiState.language == "fa") PersianDigits.toPersian(text) else text }
    }

    val averageDailyExpense = if (uiState.dayOfMonth > 0) {
        uiState.monthlyExpense / uiState.dayOfMonth
    } else {
        0L
    }

    Scaffold(
        containerColor = Color.Transparent,
        // There is no top bar on the dashboard; AppNavigation already applies the status bar
        // inset to the NavHost, so applying it here too leaves an extra gap above the greeting.
        contentWindowInsets = WindowInsets(
            top = dimensionResource(id = R.dimen.size_0dp),
            bottom = dimensionResource(id = R.dimen.size_0dp)
        )
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
            uiState.error != null -> ErrorState(
                message = uiState.error,
                onRetry = { onEvent(DashboardUiEvent.Refresh) },
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Dimens.spacingSm,
                    bottom = 136.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                item(key = "header") {
                    DashboardTopHeader(
                        greeting = stringResource(id = greetingText),
                        dateLabel = todayLabel,
                        isPrivacyEnabled = uiState.isPrivacyEnabled,
                        onTogglePrivacy = { onEvent(DashboardUiEvent.TogglePrivacy) },
                        onSettingsClick = onNavigateToSettings
                    )
                }

                if (permissionStatus.anyMissing) {
                    item(key = "permissions") {
                        PermissionsAlertCard(
                            smsMissing = !permissionStatus.smsGranted,
                            notificationsMissing = !permissionStatus.notificationsGranted,
                            needsSystemSettings = needsSystemSettings,
                            onAction = onGrantPermissions
                        )
                    }
                }

                item(key = "balance") {
                    MonetraBalanceCard(
                        totalBalanceFormatted = mask(formatMoney(uiState.totalBalance)),
                        monthlyIncomeFormatted = mask(formatMoney(uiState.monthlyIncome)),
                        monthlyExpenseFormatted = mask(formatMoney(uiState.monthlyExpense)),
                        currencyLabel = currency.code,
                        savingsRate = uiState.savingsRate,
                        savingsRateLabel = stringResource(id = R.string.dashboard_savings_rate),
                        infoChips = listOf(
                            stringResource(id = R.string.dashboard_daily_average) + ": " +
                                mask(formatMoney(averageDailyExpense)),
                            localizeNumber(
                                stringResource(
                                    id = R.string.dashboard_month_progress,
                                    uiState.dayOfMonth,
                                    uiState.daysInMonth
                                )
                            )
                        )
                    )
                }

                if (uiState.accounts.isNotEmpty()) {
                    item(key = "accounts") {
                        AccountsStrip(
                            accounts = uiState.accounts,
                            currency = currency,
                            locale = locale,
                            mask = mask,
                            onClick = onNavigateToAccounts
                        )
                    }
                }

                item(key = "quick_actions") {
                    QuickActionsCard(
                                onAddIncome = { onEvent(DashboardUiEvent.AddTransactionClicked(TransactionType.INCOME)) },
                                onAddExpense = { onEvent(DashboardUiEvent.AddTransactionClicked(TransactionType.EXPENSE)) },
                                onTransfer = { onEvent(DashboardUiEvent.AddTransactionClicked(TransactionType.TRANSFER)) }
                    )
                }

                item(key = "overview") {
                    FinancialOverviewCard(
                        trend = uiState.monthlyTrend,
                        monthlyIncome = uiState.monthlyIncome,
                        monthlyExpense = uiState.monthlyExpense,
                        currency = currency,
                        locale = locale,
                        isPrivacyEnabled = uiState.isPrivacyEnabled,
                        onClick = onNavigateToReports
                    )
                }

                if (uiState.topCategories.isNotEmpty()) {
                    item(key = "top_categories") {
                        TopSpendingCard(
                            categories = uiState.topCategories,
                            currency = currency,
                            locale = locale,
                            mask = mask,
                            onClick = onNavigateToReports
                        )
                    }
                }

                item(key = "recent_header") {
                    MonetraSectionHeader(
                        title = stringResource(id = R.string.recent_transactions),
                        actionLabel = stringResource(id = R.string.action_view_all),
                        onAction = onNavigateToTransactions
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item(key = "recent_empty") {
                        RecentTransactionsEmptyCard(
                            onAddTransaction = { onEvent(DashboardUiEvent.AddTransactionClicked(TransactionType.EXPENSE)) }
                        )
                    }
                } else {
                    items(
                        items = uiState.recentTransactions,
                        key = { transaction -> "transaction_${transaction.id}" }
                    ) { transaction ->
                        val formattedAmount = MoneyFormatter.format(transaction.amountMinorUnits, currency, locale)
                        val formattedDate = AppDateFormatter.formatDateTime(transaction.timestamp, uiState.calendar, locale)
                        val title = transaction.description?.takeIf { it.isNotBlank() }
                            ?: stringResource(id = typeLabelRes(transaction.type))

                        MonetraTransactionItem(
                            title = title,
                            subtitle = formattedDate,
                            formattedAmount = mask(formattedAmount),
                            transactionType = transaction.type,
                            categoryName = transaction.description,
                            isBankSms = transaction.source == TransactionSource.BANK_SMS,
                            onClick = { onEvent(DashboardUiEvent.TransactionClicked(transaction.id)) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alert shown at the top of the dashboard while SMS or notification access is missing.
 * It names the features that are limited and offers one action to restore access.
 */
@Composable
private fun PermissionsAlertCard(
    smsMissing: Boolean,
    notificationsMissing: Boolean,
    needsSystemSettings: Boolean,
    onAction: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = extendedColors.warningContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = extendedColors.warning,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.permission_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (smsMissing) {
                        PermissionMissingRow(
                            text = stringResource(id = R.string.permission_banner_sms_missing),
                            color = extendedColors.warning
                        )
                    }
                    if (notificationsMissing) {
                        PermissionMissingRow(
                            text = stringResource(id = R.string.permission_banner_notifications_missing),
                            color = extendedColors.warning
                        )
                    }
                    if (needsSystemSettings) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.permission_settings_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .align(Alignment.End)
                    .height(Dimens.buttonHeightSmall),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Text(
                    text = stringResource(
                        id = if (needsSystemSettings) {
                            R.string.permission_open_settings
                        } else {
                            R.string.permission_grant
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PermissionMissingRow(text: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun DashboardTopHeader(
    greeting: String,
    dateLabel: String,
    isPrivacyEnabled: Boolean,
    onTogglePrivacy: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacingXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HeaderGlassButton(
            icon = if (isPrivacyEnabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = stringResource(id = R.string.settings_privacy),
            onClick = onTogglePrivacy
        )
        Spacer(modifier = Modifier.width(8.dp))
        HeaderGlassButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(id = R.string.settings_title),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun HeaderGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    GlassSurface(
        shape = CircleShape,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Horizontally scrolling wallet summary, one frosted chip per account. */
@Composable
private fun AccountsStrip(
    accounts: List<ir.heydar.monetra.domain.model.Account>,
    currency: AppCurrency,
    locale: Locale,
    mask: (String) -> String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MonetraSectionHeader(
            title = stringResource(id = R.string.dashboard_my_accounts),
            actionLabel = stringResource(id = R.string.action_view_all),
            onAction = onClick
        )
        // The default account leads the wallet strip.
        val sortedAccounts = remember(accounts) { accounts.sortedByDescending { it.isDefault } }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(sortedAccounts, key = { it.id }) { account ->
                val accountCurrency = AppCurrency.fromCode(account.currency)
                GlassSurface(
                    modifier = Modifier.width(176.dp),
                    shape = MaterialTheme.shapes.medium,
                    onClick = onClick
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = accountTypeIcon(account.type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mask(
                                    MoneyFormatter.format(account.balance, accountCurrency, locale)
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransfer: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionTile(
                label = stringResource(id = R.string.quick_add_income),
                icon = Icons.Default.ArrowUpward,
                iconTint = extendedColors.income,
                iconBackground = extendedColors.successContainer,
                modifier = Modifier.weight(1f),
                onClick = onAddIncome
            )
            QuickActionTile(
                label = stringResource(id = R.string.quick_add_expense),
                icon = Icons.Default.ArrowDownward,
                iconTint = extendedColors.expense,
                iconBackground = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f),
                onClick = onAddExpense
            )
            QuickActionTile(
                label = stringResource(id = R.string.quick_transfer),
                icon = Icons.Default.SwapHoriz,
                iconTint = extendedColors.transfer,
                iconBackground = extendedColors.infoContainer,
                modifier = Modifier.weight(1f),
                onClick = onTransfer
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.spacingSm, horizontal = Dimens.spacingXs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = iconBackground.copy(alpha = 0.9f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinancialOverviewCard(
    trend: List<MonthlyTrendPoint>,
    monthlyIncome: Long,
    monthlyExpense: Long,
    currency: AppCurrency,
    locale: Locale,
    isPrivacyEnabled: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors
    val netSavings = monthlyIncome - monthlyExpense
    val formatMoney: (Long) -> String = remember(currency, locale) {
        { amount -> MoneyFormatter.format(amount, currency, locale) }
    }
    val mask: (String) -> String = { text -> if (isPrivacyEnabled) "••••" else text }

    // Bars grow in smoothly the first time data is available.
    var chartPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(trend) {
        if (trend.isNotEmpty()) chartPlayed = true
    }
    val chartProgress by animateFloatAsState(
        targetValue = if (chartPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "chartProgress"
    )

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.financial_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_last_months),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (trend.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacingMd))
                MonthlyTrendChart(
                    points = trend,
                    progress = chartProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // The chart is drawn left-to-right in both languages, so labels must not mirror.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        trend.forEach { point ->
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegend(color = extendedColors.income, label = stringResource(id = R.string.monthly_income))
                    ChartLegend(color = extendedColors.expense, label = stringResource(id = R.string.monthly_expense))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            HorizontalDivider(color = extendedColors.divider)
            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.analytics_net_savings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mask(formatMoney(netSavings)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netSavings >= 0) extendedColors.income else extendedColors.expense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.dashboard_open_report),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendChart(
    points: List<MonthlyTrendPoint>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val extendedColors = MaterialTheme.monetraColors
    val maxValue = remember(points) {
        points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
    }

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val slotWidth = size.width / points.size
        val barWidth = (slotWidth * 0.16f).coerceAtMost(12.dp.toPx())
        val barGap = 4.dp.toPx()
        val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        val minBarHeight = 3.dp.toPx()
        val baseline = size.height

        // Gradient bars fading toward the baseline give the chart a glassy depth.
        val incomeBrush = Brush.verticalGradient(
            listOf(extendedColors.income, extendedColors.income.copy(alpha = 0.35f))
        )
        val expenseBrush = Brush.verticalGradient(
            listOf(extendedColors.expense, extendedColors.expense.copy(alpha = 0.35f))
        )

        points.forEachIndexed { index, point ->
            val centerX = slotWidth * index + slotWidth / 2f
            val startX = centerX - (barWidth * 2 + barGap) / 2f

            if (point.income > 0L) {
                val height = max((point.income.toFloat() / maxValue) * baseline, minBarHeight) * progress
                drawRoundRect(
                    brush = incomeBrush,
                    topLeft = Offset(startX, baseline - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner
                )
            }
            if (point.expense > 0L) {
                val height = max((point.expense.toFloat() / maxValue) * baseline, minBarHeight) * progress
                drawRoundRect(
                    brush = expenseBrush,
                    topLeft = Offset(startX + barWidth + barGap, baseline - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner
                )
            }
        }
    }
}

/** This month's biggest spending categories with their share of the total. */
@Composable
private fun TopSpendingCard(
    categories: List<CategorySpending>,
    currency: AppCurrency,
    locale: Locale,
    mask: (String) -> String,
    onClick: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    var played by remember { mutableStateOf(false) }
    LaunchedEffect(categories) {
        if (categories.isNotEmpty()) played = true
    }

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = extendedColors.expense.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = extendedColors.expense,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.dashboard_top_categories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_this_month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            categories.forEach { category ->
                val progress by animateFloatAsState(
                    targetValue = if (played) category.percentage.coerceIn(0f, 1f) else 0f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    label = "categoryShare"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonetraCategoryIcon(
                        categoryName = category.name,
                        transactionType = TransactionType.EXPENSE,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.name.ifBlank { "—" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mask(
                                    MoneyFormatter.format(category.amount, currency, locale)
                                ),
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
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                extendedColors.expense,
                                                extendedColors.expense.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
                if (category != categories.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
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
private fun RecentTransactionsEmptyCard(onAddTransaction: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(
                message = stringResource(id = R.string.no_recent_transactions),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                modifier = Modifier.height(184.dp)
            )
            MonetraSecondaryButton(
                text = stringResource(id = R.string.dashboard_add_first),
                onClick = onAddTransaction,
                icon = Icons.Default.Add
            )
        }
    }
}

internal fun typeLabelRes(type: TransactionType): Int = when (type) {
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    TransactionType.TRANSFER -> R.string.transaction_type_transfer
}
