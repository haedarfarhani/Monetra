package ir.heydar.monetra.feature.transactiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.designsystem.components.AppCard
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.ErrorState
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.feature.dashboard.typeLabelRes
import java.util.Locale

@Composable
fun TransactionDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                TransactionDetailUiEffect.Deleted -> onBack()
                TransactionDetailUiEffect.NavigateToEdit -> uiState.transaction?.let { onEdit(it.id) }
            }
        }
    }

    TransactionDetailScreen(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun TransactionDetailScreen(
    uiState: TransactionDetailUiState,
    onEvent: (TransactionDetailUiEvent) -> Unit,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val locale = remember(uiState.language) { Locale.forLanguageTag(uiState.language) }
    val transaction = uiState.transaction

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.transaction_detail_title),
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (transaction != null) {
                        IconButton(onClick = { onEvent(TransactionDetailUiEvent.Edit) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.action_edit)
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.action_delete)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
            transaction == null -> ErrorState(
                message = uiState.error ?: "Transaction not found",
                onRetry = onBack,
                retryText = stringResource(id = R.string.action_cancel),
                modifier = Modifier.padding(innerPadding)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(
                            label = stringResource(id = R.string.detail_amount),
                            value = MoneyFormatter.format(transaction.amountMinorUnits, uiState.currency, locale)
                        )
                        DetailRow(
                            label = stringResource(id = R.string.detail_type),
                            value = stringResource(id = typeLabelRes(transaction.type))
                        )
                        DetailRow(
                            label = stringResource(id = R.string.detail_date),
                            value = AppDateFormatter.formatDateTime(
                                transaction.timestamp,
                                uiState.calendar,
                                locale
                            )
                        )
                        DetailRow(
                            label = stringResource(id = R.string.detail_source),
                            value = if (transaction.source == TransactionSource.BANK_SMS) {
                                stringResource(id = R.string.transactions_source_bank) +
                                    (transaction.bankName?.let { " • $it" } ?: "")
                            } else {
                                stringResource(id = R.string.transactions_source_manual)
                            }
                        )
                    }
                }

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(
                            label = stringResource(id = R.string.detail_account),
                            value = uiState.accountName.orEmpty()
                        )
                        uiState.counterAccountName?.let {
                            DetailRow(label = stringResource(id = R.string.detail_to_account), value = it)
                        }
                        uiState.categoryName?.let {
                            DetailRow(label = stringResource(id = R.string.detail_category), value = it)
                        }
                        transaction.description?.let {
                            DetailRow(label = stringResource(id = R.string.detail_description), value = it)
                        }
                        transaction.notes?.let {
                            DetailRow(label = stringResource(id = R.string.detail_notes), value = it)
                        }
                        transaction.referenceNumber?.let {
                            DetailRow(label = stringResource(id = R.string.detail_reference), value = it)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onEvent(TransactionDetailUiEvent.Delete)
                }) { Text(stringResource(id = R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
