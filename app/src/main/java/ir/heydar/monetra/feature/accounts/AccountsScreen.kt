package ir.heydar.monetra.feature.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.AppTextField
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState
import ir.heydar.monetra.core.designsystem.components.ErrorState
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.components.MonetraPrimaryButton
import ir.heydar.monetra.core.designsystem.components.accountTypeIcon
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.Account
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun AccountsRoute(
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AccountsUiEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AccountsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AccountsScreen(
    uiState: AccountsUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (AccountsUiEvent) -> Unit
) {
    val locale = remember(uiState.language) { Locale.forLanguageTag(uiState.language) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AppTopBar(title = stringResource(id = R.string.accounts_title)) },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp) // clear of the floating glass nav bar
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .clickable { onEvent(AccountsUiEvent.AddAccountClicked) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.account_add_title),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
            uiState.error != null && uiState.accounts.isEmpty() -> ErrorState(
                message = uiState.error,
                onRetry = { onEvent(AccountsUiEvent.Refresh) },
                modifier = Modifier.padding(innerPadding)
            )
            uiState.accounts.isEmpty() -> EmptyState(
                message = stringResource(id = R.string.no_accounts),
                modifier = Modifier.padding(innerPadding)
            )
            else -> AccountsList(
                uiState = uiState,
                locale = locale,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onEvent = onEvent
            )
        }
    }

    if (uiState.isSheetVisible) {
        AccountEditorSheet(
            editingAccount = uiState.editingAccount,
            defaultCurrency = uiState.defaultCurrency,
            isSaving = uiState.isSaving,
            onDismiss = { onEvent(AccountsUiEvent.DismissSheet) },
            onConfirm = { name, initialBalance, currency, type, role, bankName, accountNumber, cardNumber, holderName ->
                onEvent(
                    AccountsUiEvent.SubmitAccount(
                        name = name,
                        initialBalanceInput = initialBalance,
                        currency = currency,
                        type = type,
                        role = role,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        cardNumber = cardNumber,
                        holderName = holderName
                    )
                )
            }
        )
    }

    uiState.accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { onEvent(AccountsUiEvent.DismissDeleteConfirm) },
            containerColor = MaterialTheme.glassColors.dialogFillTop,
            title = { Text(stringResource(id = R.string.account_delete_title)) },
            text = {
                Text(
                    text = stringResource(id = R.string.account_delete_message) + "\n\n" + account.name,
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
                        .clickable { onEvent(AccountsUiEvent.DeleteConfirmed) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onEvent(AccountsUiEvent.DismissDeleteConfirm) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )
    }
}

@Composable
private fun AccountsList(
    uiState: AccountsUiState,
    locale: Locale,
    modifier: Modifier = Modifier,
    onEvent: (AccountsUiEvent) -> Unit
) {
    val formatMoney: (Account) -> String = remember(uiState.defaultCurrency, locale) {
        { account -> MoneyFormatter.format(account.balance, AppCurrency.fromCode(account.currency), locale) }
    }

    // The default account leads the list with its own hero styling.
    val sortedAccounts = remember(uiState.accounts) {
        uiState.accounts.sortedByDescending { it.isDefault }
    }

    // --- Discoverability hint: gently sway the first card once per screen open. ---
    var showHint by remember { mutableStateOf(false) }
    val hintOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(sortedAccounts.size) {
        if (sortedAccounts.isEmpty()) return@LaunchedEffect
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

    LazyColumn(
        modifier = modifier.padding(horizontal = Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        item(key = "hint") {
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(
                        shape = RoundedCornerShape(50),
                        showSheen = false
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
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
        }

        items(sortedAccounts, key = { it.id }) { account ->
            val isFirst = account.id == sortedAccounts.first().id
            SwipeableAccountItem(
                account = account,
                balanceText = formatMoney(account),
                typeLabel = typeLabelRes(account.type),
                isHero = account.isDefault,
                modifier = if (isFirst) {
                    Modifier.graphicsLayer { translationX = hintOffset.value }
                } else {
                    Modifier
                },
                onEdit = { onEvent(AccountsUiEvent.EditAccountClicked(account.id)) },
                onDelete = { onEvent(AccountsUiEvent.DeleteRequested(account.id)) }
            )
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(136.dp))
        }
    }
}

@Composable
private fun typeLabelRes(type: String): String = when (type.uppercase()) {
    "BANK" -> stringResource(id = R.string.account_type_bank)
    "CASH" -> stringResource(id = R.string.account_type_cash)
    "CARD" -> stringResource(id = R.string.account_type_card)
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAccountItem(
    account: Account,
    balanceText: String,
    typeLabel: String,
    modifier: Modifier = Modifier,
    isHero: Boolean = false,
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
            val target = dismissState.targetValue
            val extendedColors = MaterialTheme.monetraColors
            val settled = target == SwipeToDismissBoxValue.Settled

            // The card is translucent glass, so the action layer fades in only
            // while a swipe is in progress. Reading state.offset here would
            // crash before the first layout, so the reveal is driven by the
            // (composition-safe) target value instead.
            val backgroundColor by animateColorAsState(
                targetValue = when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> extendedColors.success
                    SwipeToDismissBoxValue.EndToStart -> extendedColors.expense
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(durationMillis = 180),
                label = "swipeBg"
            )
            val contentAlpha by animateFloatAsState(
                targetValue = if (settled) 0f else 1f,
                animationSpec = tween(durationMillis = 180),
                label = "swipeContent"
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
        if (isHero) {
            HeroAccountCard(
                account = account,
                balanceText = balanceText,
                typeLabel = typeLabel,
                onClick = onEdit
            )
        } else {
            AccountCard(
                account = account,
                balanceText = balanceText,
                typeLabel = typeLabel,
                onClick = onEdit
            )
        }
    }
}

/**
 * The default account, styled as the wallet's hero card with the brand
 * gradient — always the first row of the list.
 */
@Composable
private fun HeroAccountCard(
    account: Account,
    balanceText: String,
    typeLabel: String,
    onClick: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        fill = Brush.linearGradient(
            listOf(
                extendedColors.balanceGradientStart,
                extendedColors.balanceGradientMid,
                extendedColors.balanceGradientEnd
            )
        ),
        shadowElevation = 8.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color = Color.White.copy(alpha = 0.18f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = accountTypeIcon(account.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RoleBadge(
                        text = stringResource(id = R.string.account_role_default),
                        color = Color.White,
                        icon = Icons.Outlined.Star
                    )
                    when (account.purpose) {
                        Account.PURPOSE_DAILY -> RoleBadge(
                            text = stringResource(id = R.string.account_role_daily),
                            color = Color.White,
                            icon = Icons.Outlined.ShoppingCart
                        )
                        Account.PURPOSE_SALARY -> RoleBadge(
                            text = stringResource(id = R.string.account_role_salary),
                            color = Color.White,
                            icon = Icons.Outlined.Payments
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = balanceText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    balanceText: String,
    typeLabel: String,
    onClick: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (account.isDefault) {
                        RoleBadge(
                            text = stringResource(id = R.string.account_role_default),
                            color = extendedColors.success,
                            icon = Icons.Outlined.Star
                        )
                    }
                    when (account.purpose) {
                        Account.PURPOSE_DAILY -> RoleBadge(
                            text = stringResource(id = R.string.account_role_daily),
                            color = extendedColors.expense,
                            icon = Icons.Outlined.ShoppingCart
                        )
                        Account.PURPOSE_SALARY -> RoleBadge(
                            text = stringResource(id = R.string.account_role_salary),
                            color = extendedColors.warning,
                            icon = Icons.Outlined.Payments
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = balanceText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RoleBadge(
    text: String,
    color: Color,
    icon: ImageVector
) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditorSheet(
    editingAccount: Account?,
    defaultCurrency: AppCurrency,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        initialBalanceInput: String,
        currency: AppCurrency,
        type: String,
        role: String,
        bankName: String,
        accountNumber: String,
        cardNumber: String,
        holderName: String
    ) -> Unit
) {
    val isEditMode = editingAccount != null

    var type by remember(editingAccount?.id) { mutableStateOf(editingAccount?.type ?: "BANK") }
    var name by remember(editingAccount?.id) { mutableStateOf(editingAccount?.name ?: "") }
    var bankName by remember(editingAccount?.id) { mutableStateOf(editingAccount?.bankName ?: "") }
    var accountNumber by remember(editingAccount?.id) { mutableStateOf(editingAccount?.accountNumber ?: "") }
    var cardNumber by remember(editingAccount?.id) { mutableStateOf(editingAccount?.cardNumber ?: "") }
    var holderName by remember(editingAccount?.id) { mutableStateOf(editingAccount?.holderName ?: "") }
    var initialBalance by remember(editingAccount?.id) { mutableStateOf("") }
    var currency by remember(editingAccount?.id) {
        mutableStateOf(editingAccount?.let { AppCurrency.fromCode(it.currency) } ?: defaultCurrency)
    }
    var role by remember(editingAccount?.id) {
        mutableStateOf(
            when {
                editingAccount == null -> ROLE_NONE
                editingAccount.isDefault -> ROLE_DEFAULT
                editingAccount.purpose == Account.PURPOSE_DAILY -> ROLE_DAILY
                editingAccount.purpose == Account.PURPOSE_SALARY -> ROLE_SALARY
                else -> ROLE_NONE
            }
        )
    }

    val roles = listOf(
        ROLE_NONE to stringResource(id = R.string.account_role_none),
        ROLE_DEFAULT to stringResource(id = R.string.account_role_default),
        ROLE_DAILY to stringResource(id = R.string.account_role_daily),
        ROLE_SALARY to stringResource(id = R.string.account_role_salary)
    )

    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

    // Fully expanded + scrollable + keyboard-aware so nothing is ever clipped.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = accountTypeIcon(type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = stringResource(
                        id = if (isEditMode) R.string.account_edit_title else R.string.account_add_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Account type tiles — drive the detail fields below.
            Text(
                text = stringResource(id = R.string.detail_type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TypeTile(
                    label = stringResource(id = R.string.account_type_bank),
                    icon = Icons.Outlined.AccountBalance,
                    selected = type == "BANK",
                    onClick = { type = "BANK" },
                    modifier = Modifier.weight(1f)
                )
                TypeTile(
                    label = stringResource(id = R.string.account_type_card),
                    icon = Icons.Outlined.CreditCard,
                    selected = type == "CARD",
                    onClick = { type = "CARD" },
                    modifier = Modifier.weight(1f)
                )
                TypeTile(
                    label = stringResource(id = R.string.account_type_cash),
                    icon = Icons.Outlined.Payments,
                    selected = type == "CASH",
                    onClick = { type = "CASH" },
                    modifier = Modifier.weight(1f)
                )
            }

            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(id = R.string.account_name)
            )

            // Type-specific detail fields (all optional).
            when (type) {
                "BANK" -> {
                    AppTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = stringResource(id = R.string.account_field_bank_name)
                    )
                    AppTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = stringResource(id = R.string.account_field_account_number),
                        keyboardOptions = numberKeyboard
                    )
                    AppTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        label = stringResource(id = R.string.account_field_holder_name)
                    )
                }
                "CARD" -> {
                    AppTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = stringResource(id = R.string.account_field_card_number),
                        keyboardOptions = numberKeyboard
                    )
                    AppTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        label = stringResource(id = R.string.account_field_holder_name)
                    )
                }
            }

            if (isEditMode) {
                // Balance is ledger-driven, so it is shown read-only while editing.
                GlassSurface(shape = MaterialTheme.shapes.medium) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.account_current_balance),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = MoneyFormatter.format(
                                editingAccount.balance,
                                AppCurrency.fromCode(editingAccount.currency),
                                LocalLocale.current.platformLocale
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                AppTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = stringResource(id = R.string.account_initial_balance_optional),
                    keyboardOptions = numberKeyboard
                )

                Text(
                    text = stringResource(id = R.string.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCurrency.entries.forEach { option ->
                        GlassFilterChip(
                            selected = currency == option,
                            onClick = { currency = option },
                            label = { Text(option.code) }
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.account_role_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                roles.forEach { (key, label) ->
                    GlassFilterChip(
                        selected = role == key,
                        onClick = { role = key },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXs))

            MonetraPrimaryButton(
                text = stringResource(id = R.string.action_save),
                onClick = {
                    // Keep only the detail fields relevant to the chosen type.
                    onConfirm(
                        name,
                        initialBalance,
                        currency,
                        type,
                        role,
                        if (type == "BANK") bankName else "",
                        if (type == "BANK") accountNumber else "",
                        if (type == "CARD") cardNumber else "",
                        if (type == "CASH") "" else holderName
                    )
                },
                enabled = name.isNotBlank() && !isSaving,
                isLoading = isSaving
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))
        }
    }
}

/** Selectable account-type tile with an icon and a highlighted border when active. */
@Composable
private fun TypeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    GlassSurface(
        modifier = modifier
            .height(64.dp)
            .then(
                if (selected) {
                    Modifier.border(
                        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(primary, secondary))),
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.medium,
        fill = if (selected) {
            Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.22f), secondary.copy(alpha = 0.10f))
            )
        } else {
            null
        },
        showStroke = !selected,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}
