package ir.heydar.monetra.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.heydar.monetra.core.datastore.PreferencesManager
import androidx.fragment.app.FragmentActivity
import ir.heydar.monetra.core.security.BiometricManager
import ir.heydar.monetra.core.localization.LanguageManager
import ir.heydar.monetra.domain.repository.SmsImportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val languageManager: LanguageManager,
    private val smsImportRepository: SmsImportRepository,
    private val biometricManager: BiometricManager,
    @ApplicationContext context: Context
) : ViewModel() {

    /** Whether the device can perform biometric authentication. */
    val isBiometricAvailable: Boolean = biometricManager.isBiometricAvailable()

    private val _uiEffect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private val appVersion: String = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferencesManager.isBiometricEnabled,
            preferencesManager.currency,
            languageManager.observe()
        ) { biometric, currency, language ->
            Triple(biometric, currency, language)
        },
        combine(
            preferencesManager.themeMode,
            preferencesManager.calendarSystem,
            preferencesManager.autoSaveBankMessages
        ) { themeMode, calendar, autoSave ->
            Triple(themeMode, calendar, autoSave)
        },
        combine(
            preferencesManager.bankNotificationsEnabled,
            preferencesManager.bankNotificationsSound,
            preferencesManager.bankNotificationsShowAmount,
            preferencesManager.bankNotificationsHideLockscreen
        ) { notifEnabled, notifSound, showAmount, hideLockscreen ->
            NotifGroup1(notifEnabled, notifSound, showAmount, hideLockscreen)
        },
        combine(
            preferencesManager.bankNotificationsAutoSuggestCategory,
            preferencesManager.bankNotificationsAutoSuggestAccount
        ) { autoCat, autoAcc ->
            Pair(autoCat, autoAcc)
        }
    ) { base, extras, notif1, notif2 ->
        SettingsUiState(
            isBiometricEnabled = base.first,
            currency = base.second,
            language = base.third,
            themeMode = extras.first,
            calendar = extras.second,
            autoSaveBankMessages = extras.third,
            bankNotificationsEnabled = notif1.enabled,
            bankNotificationsSound = notif1.sound,
            bankNotificationsShowAmount = notif1.showAmount,
            bankNotificationsHideLockscreen = notif1.hideLockscreen,
            bankNotificationsAutoSuggestCategory = notif2.first,
            bankNotificationsAutoSuggestAccount = notif2.second,
            appVersion = appVersion
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(appVersion = appVersion)
    )

    fun onEvent(event: SettingsUiEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsUiEvent.ToggleBiometric -> preferencesManager.setBiometricEnabled(event.enabled)
                is SettingsUiEvent.ChangeCurrency -> preferencesManager.setCurrency(event.currency)
                is SettingsUiEvent.ChangeLanguage -> {
                    preferencesManager.setLanguage(event.language)
                    languageManager.apply(event.language)
                }
                is SettingsUiEvent.ChangeThemeMode -> preferencesManager.setThemeMode(event.themeMode)
                is SettingsUiEvent.ChangeCalendar -> preferencesManager.setCalendarSystem(event.calendar)
                is SettingsUiEvent.ToggleAutoSave -> preferencesManager.setAutoSaveBankMessages(event.enabled)
                is SettingsUiEvent.ToggleBankNotifications -> preferencesManager.setBankNotificationsEnabled(event.enabled)
                is SettingsUiEvent.ToggleBankNotificationsSound -> preferencesManager.setBankNotificationsSound(event.enabled)
                is SettingsUiEvent.ToggleBankNotificationsShowAmount -> preferencesManager.setBankNotificationsShowAmount(event.show)
                is SettingsUiEvent.ToggleBankNotificationsHideLockscreen -> preferencesManager.setBankNotificationsHideLockscreen(event.hide)
                is SettingsUiEvent.ToggleBankNotificationsAutoSuggestCategory -> preferencesManager.setBankNotificationsAutoSuggestCategory(event.suggest)
                is SettingsUiEvent.ToggleBankNotificationsAutoSuggestAccount -> preferencesManager.setBankNotificationsAutoSuggestAccount(event.suggest)
                is SettingsUiEvent.NotifyUnavailable ->
                    _uiEffect.send(SettingsUiEffect.ShowMessage(event.message))
                is SettingsUiEvent.ClearPendingDrafts -> {
                    runCatching { smsImportRepository.clearPending() }
                        .onSuccess { _uiEffect.send(SettingsUiEffect.DraftsCleared) }
                        .onFailure { throwable ->
                            _uiEffect.send(
                                SettingsUiEffect.ShowMessage(throwable.message ?: "Operation failed")
                            )
                        }
                }
            }
        }
    }

    /**
     * Runs the system biometric prompt. The screen passes localized strings
     * because the ViewModel has no resource access.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricManager.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    private data class NotifGroup1(
        val enabled: Boolean,
        val sound: Boolean,
        val showAmount: Boolean,
        val hideLockscreen: Boolean
    )
}
