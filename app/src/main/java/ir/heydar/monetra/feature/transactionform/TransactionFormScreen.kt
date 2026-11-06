package ir.heydar.monetra.feature.transactionform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.PersianDigits
import ir.heydar.monetra.core.designsystem.components.MonetraAmountInput
import ir.heydar.monetra.core.designsystem.components.MonetraCategoryIcon
import ir.heydar.monetra.core.designsystem.components.MonetraDatePickerDialog
import ir.heydar.monetra.core.designsystem.components.MonetraPrimaryButton
import ir.heydar.monetra.core.designsystem.components.MonetraTextField
import ir.heydar.monetra.core.designsystem.components.MonetraTimePickerDialog
import ir.heydar.monetra.core.designsystem.components.QuickDescriptionChip
import ir.heydar.monetra.core.designsystem.components.QuickDescriptionStrip
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.feature.dashboard.typeLabelRes
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun TransactionFormRoute(
    onFinished: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                TransactionFormUiEffect.Saved,
                TransactionFormUiEffect.Deleted -> onFinished()

                is TransactionFormUiEffect.Failed ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TransactionFormSheet(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onDismiss = onFinished,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    uiState: TransactionFormUiState,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TransactionFormContent(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = onDismiss,
                onDeleteClick = { showDeleteConfirm = true },
                onAddAccount = { showAddAccountDialog = true },
                onShowAllCategories = { showCategoryPicker = true },
                onShowDatePicker = { showDatePicker = true },
                onShowTimePicker = { showTimePicker = true }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = uiState.categoriesForType,
            type = uiState.type,
            selectedId = uiState.categoryId,
            onSelect = { categoryId ->
                showCategoryPicker = false
                onEvent(TransactionFormUiEvent.SelectCategory(categoryId))
            },
            onAddNew = {
                showCategoryPicker = false
                showNewCategoryDialog = true
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.glassColors.dialogFillTop,
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onEvent(TransactionFormUiEvent.Delete)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showNewCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onConfirm = { name ->
                showNewCategoryDialog = false
                onEvent(TransactionFormUiEvent.CreateCategory(name))
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            currency = uiState.currency,
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, balanceInput, accountType ->
                showAddAccountDialog = false
                onEvent(TransactionFormUiEvent.CreateAccount(name, balanceInput, accountType))
            }
        )
    }

    if (showDatePicker) {
        val locale = formLocale(uiState.language)
        MonetraDatePickerDialog(
            initialTimestamp = uiState.timestamp,
            initialCalendar = uiState.calendar,
            locale = locale,
            onDateSelected = { onEvent(TransactionFormUiEvent.ChangeDate(it)) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val locale = formLocale(uiState.language)
        val time = remember(uiState.timestamp) {
            Instant.ofEpochMilli(uiState.timestamp).atZone(ZoneId.systemDefault())
        }
        MonetraTimePickerDialog(
            initialHour = time.hour,
            initialMinute = time.minute,
            locale = locale,
            onTimeSelected = { hour, minute ->
                onEvent(TransactionFormUiEvent.ChangeTime(hour, minute))
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

private fun formLocale(language: String): Locale =
    if (language.isBlank()) Locale.getDefault() else Locale.forLanguageTag(language)

@Composable
private fun TransactionFormContent(
    uiState: TransactionFormUiState,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddAccount: () -> Unit,
    onShowAllCategories: () -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit
) {
    val locale = remember(uiState.language) { formLocale(uiState.language) }
    val context = LocalContext.current
    val quickChips = remember(context, uiState.language) {
        context.resources.getStringArray(R.array.transaction_quick_descriptions)
            .mapIndexed { index, label ->
                QuickDescriptionChip(labelResId = index, localizedLabel = label)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        // Header: title + delete (edit mode) + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    id = if (uiState.isEditing) {
                        R.string.transaction_form_edit_title
                    } else {
                        R.string.transaction_form_add_title
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                if (uiState.isEditing) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1) Transaction type segmented control
        TransactionTypeSelector(
            selected = uiState.type,
            onSelect = { onEvent(TransactionFormUiEvent.ChangeType(it)) }
        )

        // 2) Amount
        MonetraAmountInput(
            value = uiState.amountInput,
            onValueChange = { onEvent(TransactionFormUiEvent.ChangeAmount(it)) },
            currencyCode = uiState.currency.code,
            transactionType = uiState.type,
            fractionDigits = uiState.currency.fractionDigits,
            locale = locale,
            isError = uiState.amountError,
            errorMessage = stringResource(id = R.string.error_amount_required)
        )

        // 3) Account — one-tap chip selection
        SectionLabel(
            text = stringResource(
                id = if (uiState.isTransfer) R.string.field_from_account else R.string.field_account
            ),
            isError = uiState.accountError
        )
        AccountChipRow(
            accounts = uiState.accounts,
            selectedId = uiState.accountId,
            onSelect = { onEvent(TransactionFormUiEvent.SelectAccount(it)) },
            onAdd = onAddAccount
        )
        if (uiState.accountError) {
            FieldError(text = stringResource(id = R.string.error_account_required))
        }

        if (uiState.isTransfer) {
            SectionLabel(text = stringResource(id = R.string.field_to_account), isError = uiState.counterAccountError)
            AccountChipRow(
                accounts = uiState.accounts.filter { it.id != uiState.accountId },
                selectedId = uiState.counterAccountId,
                onSelect = { onEvent(TransactionFormUiEvent.SelectCounterAccount(it)) },
                onAdd = onAddAccount
            )
            if (uiState.counterAccountError) {
                FieldError(text = stringResource(id = R.string.error_accounts_must_differ))
            }
        } else {
            // 4) Category — one-tap chips on a single scrollable line, full grid in the picker
            SectionLabel(text = stringResource(id = R.string.field_category), isError = uiState.categoryError)
            CategoryChipRow(
                categories = uiState.categoriesForType,
                type = uiState.type,
                selectedId = uiState.categoryId,
                onSelect = { onEvent(TransactionFormUiEvent.SelectCategory(it)) },
                onShowAll = onShowAllCategories
            )
            if (uiState.categoryError) {
                FieldError(text = stringResource(id = R.string.error_category_required))
            }
        }

        // 5) Date & time — above the description
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            PickerField(
                label = stringResource(id = R.string.field_date),
                value = AppDateFormatter.formatDate(uiState.timestamp, uiState.calendar, locale),
                modifier = Modifier.weight(1f),
                onClick = onShowDatePicker
            )
            PickerField(
                label = stringResource(id = R.string.field_time),
                value = formatTime(uiState.timestamp, uiState.language, locale),
                modifier = Modifier.weight(1f),
                onClick = onShowTimePicker,
                isDate = false
            )
        }

        // 6) Single merged description (شرح = توضیحات)
        QuickDescriptionStrip(
            items = quickChips,
            selectedLabel = uiState.description,
            onSelect = { onEvent(TransactionFormUiEvent.ChangeDescription(it)) }
        )
        MonetraTextField(
            value = uiState.description,
            onValueChange = { onEvent(TransactionFormUiEvent.ChangeDescription(it)) },
            label = stringResource(id = R.string.field_description),
            singleLine = false,
            maxLines = 3
        )

        if (uiState.accounts.isEmpty()) {
            Text(
                text = stringResource(id = R.string.error_no_accounts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        MonetraPrimaryButton(
            text = stringResource(id = R.string.action_save),
            onClick = { onEvent(TransactionFormUiEvent.Save) },
            enabled = !uiState.isLoading && uiState.accounts.isNotEmpty(),
            isLoading = uiState.isSaving
        )
    }
}

@Composable
private fun SectionLabel(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/** Segmented INCOME / EXPENSE / TRANSFER control, tinted with each type's semantic color. */
@Composable
private fun TransactionTypeSelector(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.glassColors.innerFillTop)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selected
            val tint = when (type) {
                TransactionType.INCOME -> extendedColors.income
                TransactionType.EXPENSE -> extendedColors.expense
                TransactionType.TRANSFER -> extendedColors.transfer
            }
            Text(
                text = stringResource(id = typeLabelRes(type)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) tint else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

/** Horizontally scrollable one-tap account chips with a trailing "add" chip. */
@Composable
private fun AccountChipRow(
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { account ->
            SelectableChip(
                selected = account.id == selectedId,
                onClick = { onSelect(account.id) },
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = account.name
            )
        }
        AddChip(
            label = stringResource(id = R.string.account_add_title),
            onClick = onAdd
        )
    }
}

/**
 * One-tap category chips on a single horizontal line; the selected category is moved to the
 * front so it is always visible. "All categories" opens the full grid picker.
 */
@Composable
private fun CategoryChipRow(
    categories: List<Category>,
    type: TransactionType,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onShowAll: () -> Unit
) {
    val ordered = remember(categories, selectedId) {
        categories.sortedByDescending { it.id == selectedId }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ordered.forEach { category ->
            SelectableChip(
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                leading = {
                    MonetraCategoryIcon(
                        categoryName = category.name,
                        transactionType = type,
                        size = 18.dp
                    )
                },
                label = category.name
            )
        }
        AddChip(
            label = stringResource(id = R.string.action_all_categories),
            leading = {
                Icon(
                    imageVector = Icons.Outlined.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = onShowAll
        )
    }
}

/** Full grid of categories (icon + name); tapping one selects it and closes the dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    type: TransactionType,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        title = { Text(stringResource(id = R.string.field_category)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    SelectableChip(
                        selected = category.id == selectedId,
                        onClick = { onSelect(category.id) },
                        leading = {
                            MonetraCategoryIcon(
                                categoryName = category.name,
                                transactionType = type,
                                size = 18.dp
                            )
                        },
                        label = category.name
                    )
                }
                AddChip(
                    label = stringResource(id = R.string.action_new_category),
                    leading = {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = onAddNew
                )
            }
        },
        confirmButton = {
            // Selection applies instantly on tap; this just closes the picker.
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_confirm))
            }
        }
    )
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.glassColors.innerFillTop
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddChip(
    label: String,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color.Transparent)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.glassColors.strokeEnd,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading == null) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            leading()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDate: Boolean = true
) {
    GlassSurface(
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDate) Icons.Outlined.CalendarToday else Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSizeSm)
            )
            Spacer(modifier = Modifier.width(Dimens.spacingXs))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        title = { Text(stringResource(id = R.string.dialog_new_category_title)) },
        text = {
            MonetraTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(id = R.string.field_category_name)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}

private fun formatTime(timestamp: Long, language: String, locale: Locale): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val text = "%02d:%02d".format(time.hour, time.minute)
    return if (language == "fa" || locale.language == "fa") {
        PersianDigits.toPersian(text)
    } else {
        text
    }
}
