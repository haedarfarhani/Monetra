package ir.heydar.monetra.feature.bankreview

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.designsystem.components.AppCard
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.MonetraCategoryIcon
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.domain.model.TransactionType

@Composable
fun BankTransactionReviewRoute(
    onDone: () -> Unit,
    viewModel: BankTransactionReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BankTransactionReviewScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onDone = onDone
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankTransactionReviewScreen(
    uiState: BankTransactionReviewUiState,
    onEvent: (BankTransactionReviewUiEvent) -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.review_screen_title),
                showBackButton = true,
                onBackClick = onDone
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.status) {
                ReviewScreenStatus.LOADING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                ReviewScreenStatus.CONFIRMED -> {
                    StatusView(
                        icon = Icons.Default.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        title = stringResource(id = R.string.review_confirmed_title),
                        description = stringResource(id = R.string.review_confirmed_desc),
                        buttonText = stringResource(id = R.string.action_finish),
                        onButtonClick = onDone
                    )
                }
                ReviewScreenStatus.REJECTED -> {
                    StatusView(
                        icon = Icons.Default.Done,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = stringResource(id = R.string.review_rejected_title),
                        description = stringResource(id = R.string.review_rejected_desc),
                        buttonText = stringResource(id = R.string.action_finish),
                        onButtonClick = onDone
                    )
                }
                ReviewScreenStatus.EXPIRED -> {
                    StatusView(
                        icon = Icons.Default.Warning,
                        tint = MaterialTheme.colorScheme.error,
                        title = stringResource(id = R.string.review_expired_title),
                        description = stringResource(id = R.string.review_expired_desc),
                        buttonText = stringResource(id = R.string.action_back),
                        onButtonClick = onDone
                    )
                }
                ReviewScreenStatus.ERROR -> {
                    StatusView(
                        icon = Icons.Default.Warning,
                        tint = MaterialTheme.colorScheme.error,
                        title = stringResource(id = R.string.error_generic),
                        description = uiState.errorMessage ?: "",
                        buttonText = stringResource(id = R.string.action_back),
                        onButtonClick = onDone
                    )
                }
                ReviewScreenStatus.READY -> {
                    val pending = uiState.pending ?: return@Box
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        // Summary Card
                        AppCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = pending.bankName ?: stringResource(id = R.string.detail_bank),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = uiState.amountInput,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (uiState.type) {
                                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    pending.cardLastFour?.let { lastFour ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "•••• $lastFour",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }

                                    pending.transactionTimestamp?.let { ts ->
                                        val dateText = AppDateFormatter.formatDate(ts, uiState.calendar)
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                pending.referenceNumber?.let { ref ->
                                    Text(
                                        text = "${stringResource(id = R.string.field_reference)}: $ref",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Account Selection Card
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(id = R.string.field_account),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                var accountExpanded by remember { mutableStateOf(false) }
                                val selectedAccount = uiState.accounts.firstOrNull { it.id == uiState.selectedAccountId }

                                ExposedDropdownMenuBox(
                                    expanded = accountExpanded,
                                    onExpandedChange = { accountExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedAccount?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = accountExpanded,
                                        onDismissRequest = { accountExpanded = false }
                                    ) {
                                        uiState.accounts.forEach { account ->
                                            DropdownMenuItem(
                                                text = { Text(account.name) },
                                                onClick = {
                                                    onEvent(BankTransactionReviewUiEvent.SelectAccount(account.id))
                                                    accountExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Category Selection Card (Optional)
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(id = R.string.field_category),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                var categoryExpanded by remember { mutableStateOf(false) }
                                val selectedCategory = uiState.categories.firstOrNull { it.id == uiState.selectedCategoryId }

                                ExposedDropdownMenuBox(
                                    expanded = categoryExpanded,
                                    onExpandedChange = { categoryExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCategory?.name ?: stringResource(id = R.string.filter_all),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                        leadingIcon = {
                                            MonetraCategoryIcon(
                                                categoryName = selectedCategory?.name,
                                                transactionType = uiState.type,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = categoryExpanded,
                                        onDismissRequest = { categoryExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.action_clear)) },
                                            onClick = {
                                                onEvent(BankTransactionReviewUiEvent.SelectCategory(null))
                                                categoryExpanded = false
                                            }
                                        )
                                        uiState.categoriesForType.forEach { cat ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    MonetraCategoryIcon(
                                                        categoryName = cat.name,
                                                        transactionType = uiState.type,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                text = { Text(cat.name) },
                                                onClick = {
                                                    onEvent(BankTransactionReviewUiEvent.SelectCategory(cat.id))
                                                    categoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Description Field
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(id = R.string.field_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = { onEvent(BankTransactionReviewUiEvent.ChangeDescription(it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Button(
                            onClick = { onEvent(BankTransactionReviewUiEvent.Confirm) },
                            enabled = !uiState.isProcessing && uiState.selectedAccountId != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.action_confirm),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onEvent(BankTransactionReviewUiEvent.Reject) },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.action_delete),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onButtonClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(buttonText)
        }
    }
}
