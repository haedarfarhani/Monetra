package ir.heydar.monetra.feature.smsimport

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.components.AppCard
import ir.heydar.monetra.core.designsystem.components.AppOutlinedButton
import ir.heydar.monetra.core.designsystem.components.AppTextField
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.MonetraPrimaryButton
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.feature.dashboard.typeLabelRes

@Composable
fun SmsImportRoute(
    onDone: () -> Unit,
    viewModel: SmsImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SmsImportScreen(uiState = uiState, onEvent = viewModel::onEvent, onDone = onDone)
}

@Composable
fun SmsImportScreen(
    uiState: SmsImportUiState,
    onEvent: (SmsImportUiEvent) -> Unit,
    onDone: () -> Unit
) {
    val extendedColors = MaterialTheme.monetraColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.sms_import_title),
                showBackButton = true,
                onBackClick = onDone
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // Paste message field
            AppTextField(
                value = uiState.input,
                onValueChange = { onEvent(SmsImportUiEvent.InputChanged(it)) },
                label = stringResource(id = R.string.sms_import_paste_label),
                singleLine = false,
                maxLines = 4
            )

            MonetraPrimaryButton(
                text = stringResource(id = R.string.sms_import_parse),
                onClick = { onEvent(SmsImportUiEvent.Analyse) },
                enabled = uiState.input.isNotBlank() && !uiState.isAnalysing,
                isLoading = uiState.isAnalysing
            )

            // Status feedback banners
            when (uiState.status) {
                SmsImportStatus.NOT_FINANCIAL -> StatusCard(
                    text = stringResource(id = R.string.sms_import_not_financial),
                    icon = Icons.Default.Warning,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                SmsImportStatus.DUPLICATE -> StatusCard(
                    text = stringResource(id = R.string.sms_import_duplicate),
                    icon = Icons.Default.Warning,
                    containerColor = extendedColors.warningContainer,
                    contentColor = extendedColors.onWarning
                )
                SmsImportStatus.SAVED -> StatusCard(
                    text = stringResource(id = R.string.sms_import_saved),
                    icon = Icons.Default.CheckCircle,
                    containerColor = extendedColors.successContainer,
                    contentColor = extendedColors.onSuccess
                )
                SmsImportStatus.FAILED -> StatusCard(
                    text = stringResource(id = R.string.error_generic),
                    icon = Icons.Default.Warning,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                else -> Unit
            }

            val draft = uiState.draft
            if (draft != null && uiState.status != SmsImportStatus.NOT_FINANCIAL) {
                // Draft Card with confidence badge
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                                Text(
                                    text = draft.bankName ?: stringResource(id = R.string.sms_import_bank),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Confidence Badge
                            val confidenceColor = when {
                                draft.confidence >= 80 -> extendedColors.success
                                draft.confidence >= 50 -> extendedColors.warning
                                else -> MaterialTheme.colorScheme.error
                            }
                            Surface(
                                shape = CircleShape,
                                color = confidenceColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${draft.confidence}% ${stringResource(id = R.string.sms_import_confidence)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = confidenceColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (draft.cardLastFourDigits != null) {
                            Text(
                                text = "${stringResource(id = R.string.sms_import_card)}: ****${draft.cardLastFourDigits}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (draft.accountBalanceMinorUnits != null) {
                            Text(
                                text = "${stringResource(id = R.string.sms_import_balance)}: " +
                                    MoneyFormatter.format(draft.accountBalanceMinorUnits, uiState.currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Transaction Type selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { type ->
                        GlassFilterChip(
                            selected = uiState.type == type,
                            onClick = { onEvent(SmsImportUiEvent.ChangeType(type)) },
                            label = { Text(stringResource(id = typeLabelRes(type))) }
                        )
                    }
                }

                // Amount
                AppTextField(
                    value = uiState.amountInput,
                    onValueChange = { onEvent(SmsImportUiEvent.ChangeAmount(it)) },
                    label = stringResource(id = R.string.field_amount) + " (${uiState.currency.code})"
                )

                // Account Selection Chips
                Text(
                    text = stringResource(id = R.string.field_account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { account ->
                        GlassFilterChip(
                            selected = uiState.accountId == account.id,
                            onClick = { onEvent(SmsImportUiEvent.SelectAccount(account.id)) },
                            label = { Text(account.name) }
                        )
                    }
                }

                // Description
                AppTextField(
                    value = uiState.description,
                    onValueChange = { onEvent(SmsImportUiEvent.ChangeDescription(it)) },
                    label = stringResource(id = R.string.field_description)
                )

                Spacer(modifier = Modifier.height(Dimens.spacingSm))

                // Actions: Save & Reject
                MonetraPrimaryButton(
                    text = stringResource(id = R.string.sms_import_save),
                    onClick = { onEvent(SmsImportUiEvent.Save) },
                    enabled = !uiState.isSaving && uiState.status != SmsImportStatus.DUPLICATE,
                    isLoading = uiState.isSaving
                )

                AppOutlinedButton(
                    text = stringResource(id = R.string.sms_import_reject),
                    onClick = { onEvent(SmsImportUiEvent.Reject) }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Privacy Assurance Footer
            Text(
                text = stringResource(id = R.string.sms_import_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))
        }
    }
}

@Composable
private fun StatusCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
