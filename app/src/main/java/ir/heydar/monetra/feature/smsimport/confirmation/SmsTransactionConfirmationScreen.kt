package ir.heydar.monetra.feature.smsimport.confirmation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.components.AppPrimaryButton
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.MonetraSecondaryButton

@Composable
fun SmsTransactionConfirmationRoute(
    onDone: () -> Unit,
    viewModel: SmsTransactionConfirmationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SmsTransactionConfirmationScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onDone = onDone
    )
}

@Composable
fun SmsTransactionConfirmationScreen(
    uiState: SmsTransactionConfirmationUiState,
    onEvent: (SmsTransactionConfirmationUiEvent) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(id = R.string.review_screen_title),
            onBackClick = onDone
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.status) {
                ConfirmationStatus.LOADING -> CircularProgressIndicator()
                ConfirmationStatus.CONFIRMED, ConfirmationStatus.REJECTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.status == ConfirmationStatus.CONFIRMED) {
                                stringResource(id = R.string.review_confirmed_title)
                            } else {
                                stringResource(id = R.string.review_rejected_title)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AppPrimaryButton(
                            text = stringResource(id = R.string.action_finish),
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        uiState.pending?.let { pending ->
                            Text(
                                text = pending.bankName ?: stringResource(id = R.string.notif_bank_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${pending.amountMinorUnits} ${pending.currencyCode}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        AppPrimaryButton(
                            text = stringResource(id = R.string.notif_bank_confirm),
                            onClick = { onEvent(SmsTransactionConfirmationUiEvent.Confirm) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MonetraSecondaryButton(
                            text = stringResource(id = R.string.notif_bank_reject),
                            onClick = { onEvent(SmsTransactionConfirmationUiEvent.Reject) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
