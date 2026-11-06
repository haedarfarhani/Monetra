package ir.heydar.monetra.feature.transactionform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.R
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.MonetraTextField
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import ir.heydar.monetra.core.text.TextNormalizer

/**
 * Creates a new account without leaving the transaction form.
 *
 * [onConfirm] receives the raw balance text so the caller can parse it with the currency that the
 * form is currently using.
 */
@Composable
fun AddAccountDialog(
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balanceInput: String, accountType: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(ACCOUNT_TYPE_BANK) }

    val balanceValid = balanceInput.isBlank() ||
        MoneyFormatter.parseToMinorUnits(balanceInput, currency) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        title = { Text(stringResource(id = R.string.account_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                MonetraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(id = R.string.account_name)
                )
                MonetraTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = sanitizeBalanceInput(it, currency.fractionDigits) },
                    label = stringResource(id = R.string.account_initial_balance),
                    placeholder = "0",
                    isError = !balanceValid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (currency.fractionDigits > 0) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Number
                        }
                    ),
                    trailingIcon = {
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Text(
                    text = stringResource(id = R.string.account_type_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    AccountTypeOption(
                        label = stringResource(id = R.string.account_type_bank),
                        icon = Icons.Outlined.AccountBalance,
                        selected = accountType == ACCOUNT_TYPE_BANK,
                        modifier = Modifier.weight(1f),
                        onClick = { accountType = ACCOUNT_TYPE_BANK }
                    )
                    AccountTypeOption(
                        label = stringResource(id = R.string.account_type_cash),
                        icon = Icons.Outlined.Payments,
                        selected = accountType == ACCOUNT_TYPE_CASH,
                        modifier = Modifier.weight(1f),
                        onClick = { accountType = ACCOUNT_TYPE_CASH }
                    )
                    AccountTypeOption(
                        label = stringResource(id = R.string.account_type_card),
                        icon = Icons.Outlined.CreditCard,
                        selected = accountType == ACCOUNT_TYPE_CARD,
                        modifier = Modifier.weight(1f),
                        onClick = { accountType = ACCOUNT_TYPE_CARD }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), balanceInput, accountType) },
                enabled = name.isNotBlank() && balanceValid
            ) { Text(stringResource(id = R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}

@Composable
private fun AccountTypeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}

private fun sanitizeBalanceInput(input: String, fractionDigits: Int): String {
    val cleaned = TextNormalizer.normalize(input).filter { it.isDigit() || it == '.' }
    val parts = cleaned.split('.')
    val integerPart = parts.firstOrNull()?.filter { it.isDigit() }.orEmpty()
    if (fractionDigits == 0) return integerPart

    val decimalPart = parts.getOrNull(1)?.filter { it.isDigit() }?.take(fractionDigits).orEmpty()
    return if (cleaned.contains('.')) "$integerPart.$decimalPart" else integerPart
}

private const val ACCOUNT_TYPE_BANK = "BANK"
private const val ACCOUNT_TYPE_CASH = "CASH"
private const val ACCOUNT_TYPE_CARD = "CARD"
