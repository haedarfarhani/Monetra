package ir.heydar.monetra.feature.smsimport.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.feature.smsimport.receiver.SmsReceptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsSettingsUiState(
    val hasSmsPermission: Boolean = false,
    val isAutoDetectionEnabled: Boolean = true,
    val policyNotice: String = ""
)

@HiltViewModel
class SmsSettingsViewModel @Inject constructor(
    private val receptionManager: SmsReceptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsSettingsUiState())
    val uiState: StateFlow<SmsSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            receptionManager.isAutoDetectionEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(
                        hasSmsPermission = receptionManager.hasSmsPermission(),
                        isAutoDetectionEnabled = enabled,
                        policyNotice = receptionManager.getGooglePlayPolicyNotice()
                    )
                }
            }
        }
    }

    fun toggleAutoDetection(enabled: Boolean) {
        viewModelScope.launch {
            receptionManager.setAutoDetectionEnabled(enabled)
            _uiState.update { it.copy(isAutoDetectionEnabled = enabled) }
        }
    }
}
