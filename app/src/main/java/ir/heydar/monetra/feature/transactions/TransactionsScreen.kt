package ir.heydar.monetra.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.AppTextField
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState
import ir.heydar.monetra.core.designsystem.components.ErrorState
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.components.MonetraPrimaryButton
import ir.heydar.monetra.core.designsystem.components.MonetraTransactionItem
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.feature.dashboard.typeLabelRes
import java.util.Locale

@Composable
fun TransactionsRoute(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is TransactionsUiEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is TransactionsUiEffect.NavigateToAdd -> onNavigateToAdd()
            }
        }
    }

    TransactionsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToEdit = onNavigateToEdit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onEvent: (TransactionsUiEvent) -> Unit,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    var showFilters by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<ir.heydar.monetra.domain.model.Transaction?>(null) }
    val listState = rememberLazyListState()
    val locale = remember(uiState.language) { Locale.forLanguageTag(uiState.language) }

    // --- Discoverability hint: gently sway the first transaction card once per open. ---
    val firstTransactionId = uiState.groups.firstOrNull()?.transactions?.firstOrNull()?.id
    var showHint by remember { mutableStateOf(false) }
    val hintOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(firstTransactionId) {
        if (firstTransactionId == null) return@LaunchedEffect
        delay(600)
        showHint = true
        val distance = with(density) { 44.dp.toPx() }
        val sway = tween<Float>(450, easing = FastOutSlowInEasing)
        hintOffset.animateTo(distance, sway)
        hintOffset.animateTo(-distance, tween(650, easing = FastOutSlowInEasing))
        hintOffset.animateTo(distance / 2f, sway)
        hintOffset.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        showHint = false
    }

    LaunchedEffect(listState, uiState.hasMore, uiState.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (uiState.hasMore && lastVisible >= listState.layoutInfo.totalItemsCount - 3) {
                    onEvent(TransactionsUiEvent.LoadMore)
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.transactions_title),
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.transactions_filters),
                            tint = if (uiState.hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input with Search & Clear icon
            AppTextField(
                value = uiState.search,
                onValueChange = { onEvent(TransactionsUiEvent.SearchChanged(it)) },
                label = stringResource(id = R.string.transactions_search_hint),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.search.isNotEmpty()) {
                        IconButton(onClick = { onEvent(TransactionsUiEvent.SearchChanged("")) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingSm)
            )

            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(shape = RoundedCornerShape(50), showSheen = false) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.accounts_swipe_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error,
                    onRetry = { onEvent(TransactionsUiEvent.Refresh) }
                )
                uiState.groups.isEmpty() -> EmptyState(
                    message = stringResource(
                        id = if (uiState.hasActiveFilters) {
                            R.string.transactions_no_results
                        } else {
                            R.string.no_transactions
                        }
                    )
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Dimens.screenPadding,
                        end = Dimens.screenPadding,
                        top = Dimens.spacingSm,
                        bottom = 136.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.groups.forEach { group ->
                        item(key = "header-${group.dateLabel}") {
                            DateGroupHeader(
                                group = group,
                                currency = uiState.currency,
                                locale = locale
                            )
                        }
                        items(
                            count = group.transactions.size,
                            key = { index -> group.transactions[index].id }
                        ) { index ->
                            val transaction = group.transactions[index]
                            val accountName = uiState.accounts.firstOrNull { it.id == transaction.accountId }?.name
                            val categoryName = transaction.categoryId?.let { categoryId ->
                                uiState.categories.firstOrNull { it.id == categoryId }?.name
                            }
                            val formattedAmount = MoneyFormatter.format(transaction.amountMinorUnits, AppCurrency.fromCode(transaction.currencyCode), locale)
                            val title = transaction.description?.takeIf { it.isNotBlank() }
                                ?: categoryName
                                ?: stringResource(id = typeLabelRes(transaction.type))
                            val subtitle = buildString {
                                accountName?.let { append(it) }
                                if (isEmpty()) append(stringResource(id = typeLabelRes(transaction.type)))
                                append(" • ")
                                append(AppDateFormatter.formatDateTime(transaction.timestamp, uiState.calendar, locale))
                            }

                            SwipeableTransactionItem(
                                modifier = if (transaction.id == firstTransactionId) {
                                    Modifier.graphicsLayer { translationX = hintOffset.value }
                                } else {
                                    Modifier
                                },
                                title = title,
                                subtitle = subtitle,
                                formattedAmount = formattedAmount,
                                transactionType = transaction.type,
                                categoryName = categoryName ?: transaction.description,
                                isBankSms = transaction.source == TransactionSource.BANK_SMS,
                                onClick = { onEvent(TransactionsUiEvent.TransactionClicked(transaction.id)) },
                                onEdit = { onNavigateToEdit(transaction.id) },
                                onDelete = { transactionToDelete = transaction }
                            )
                        }
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transactions_load_more),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.glassColors.dialogFillTop,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            FiltersContent(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = { showFilters = false }
            )
        }
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = MaterialTheme.glassColors.dialogFillTop,
            title = { Text(stringResource(id = R.string.action_delete)) },
            text = {
                val description = transaction.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { suffix -> "\n\n" + suffix }
                    .orEmpty()
                Text(
                    text = stringResource(id = R.string.transaction_delete_message) + description,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Text(
                    text = stringResource(id = R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            onEvent(TransactionsUiEvent.DeleteTransaction(transaction.id))
                            transactionToDelete = null
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { transactionToDelete = null }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )
    }
}

@Composable
private fun DateGroupHeader(
    group: TransactionGroup,
    currency: AppCurrency,
    locale: Locale
) {
    val extendedColors = MaterialTheme.monetraColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.spacingMd, bottom = Dimens.spacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.dateLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (group.incomeMinorUnits != 0L || group.expenseMinorUnits != 0L) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.incomeMinorUnits > 0L) {
                    Text(
                        text = "+${MoneyFormatter.format(group.incomeMinorUnits, currency, locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = extendedColors.income
                    )
                }
                if (group.incomeMinorUnits > 0L && group.expenseMinorUnits > 0L) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (group.expenseMinorUnits > 0L) {
                    Text(
                        text = "−${MoneyFormatter.format(group.expenseMinorUnits, currency, locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = extendedColors.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersContent(
    uiState: TransactionsUiState,
    onEvent: (TransactionsUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingMd)
    ) {
        Text(
            text = stringResource(id = R.string.transactions_filters),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMd))

        // Transaction Type Filter Chips
        Text(
            text = stringResource(id = R.string.detail_type),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.spacingXs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassFilterChip(
                selected = uiState.typeFilter == null,
                onClick = { onEvent(TransactionsUiEvent.TypeFilterChanged(null)) },
                label = { Text(stringResource(id = R.string.filter_all)) }
            )
            TransactionType.entries.forEach { type ->
                GlassFilterChip(
                    selected = uiState.typeFilter == type,
                    onClick = { onEvent(TransactionsUiEvent.TypeFilterChanged(type)) },
                    label = { Text(stringResource(id = typeLabelRes(type))) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingMd))

        // Account Filter
        DropdownRow(
            label = stringResource(id = R.string.filter_account),
            selectedLabel = uiState.accounts.firstOrNull { it.id == uiState.accountFilter }?.name
                ?: stringResource(id = R.string.filter_all),
            options = uiState.accounts.map { it.id to it.name },
            onSelect = { onEvent(TransactionsUiEvent.AccountFilterChanged(it)) }
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        // Category Filter
        DropdownRow(
            label = stringResource(id = R.string.filter_category),
            selectedLabel = uiState.categories.firstOrNull { it.id == uiState.categoryFilter }?.name
                ?: stringResource(id = R.string.filter_all),
            options = uiState.categories.map { it.id to it.name },
            onSelect = { onEvent(TransactionsUiEvent.CategoryFilterChanged(it)) }
        )

        Spacer(modifier = Modifier.height(Dimens.spacingLg))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onEvent(TransactionsUiEvent.ClearFilters) }) {
                Text(stringResource(id = R.string.action_clear))
            }
            Spacer(modifier = Modifier.width(8.dp))
            MonetraPrimaryButton(
                text = stringResource(id = R.string.action_save),
                onClick = onDismiss,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

@Composable
private fun DropdownRow(
    label: String,
    selectedLabel: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.filter_all)) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
                options.forEach { (id, name) ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Transaction row with swipe-to-edit (right) and swipe-to-delete (left).
 * The actions snap the row back; delete asks for confirmation first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    formattedAmount: String,
    transactionType: TransactionType,
    categoryName: String?,
    isBankSms: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Always snap back into place; the action itself is handled above.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val extendedColors = MaterialTheme.monetraColors
            val target = dismissState.targetValue
            val settled = target == SwipeToDismissBoxValue.Settled
            val backgroundColor by animateColorAsState(
                targetValue = when {
                    settled -> Color.Transparent
                    target == SwipeToDismissBoxValue.StartToEnd -> extendedColors.success
                    else -> extendedColors.expense
                },
                animationSpec = tween(180),
                label = "txSwipeBg"
            )
            val contentAlpha by animateFloatAsState(
                targetValue = if (settled) 0f else 1f,
                animationSpec = tween(180),
                label = "txSwipeContent"
            )
            val (icon, alignment) = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit to Alignment.CenterStart
                else -> Icons.Outlined.Delete to Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(backgroundColor),
                contentAlignment = alignment
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(
                            id = if (target == SwipeToDismissBoxValue.StartToEnd) {
                                R.string.action_edit
                            } else {
                                R.string.action_delete
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) {
        MonetraTransactionItem(
            title = title,
            subtitle = subtitle,
            formattedAmount = formattedAmount,
            transactionType = transactionType,
            categoryName = categoryName,
            isBankSms = isBankSms,
            onClick = onClick
        )
    }
}
