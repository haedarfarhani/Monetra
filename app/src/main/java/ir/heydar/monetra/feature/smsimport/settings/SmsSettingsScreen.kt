package ir.heydar.monetra.feature.smsimport.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import ir.heydar.monetra.core.designsystem.components.AppCard
import ir.heydar.monetra.core.designsystem.components.AppTopBar

@Composable
fun SmsSettingsRoute(
    onBackClick: () -> Unit,
    viewModel: SmsSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SmsSettingsScreen(
        uiState = uiState,
        onToggleAutoDetection = viewModel::toggleAutoDetection,
        onBackClick = onBackClick
    )
}

@Composable
fun SmsSettingsScreen(
    uiState: SmsSettingsUiState,
    onToggleAutoDetection: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(id = R.string.settings_bank_notifications),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_bank_notif_enable),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.isAutoDetectionEnabled,
                            onCheckedChange = onToggleAutoDetection
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.policyNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
