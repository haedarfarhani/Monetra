package ir.heydar.monetra.feature.settings

sealed interface SettingsUiEvent {
    data class ToggleBiometric(val enabled: Boolean) : SettingsUiEvent
    data class ChangeCurrency(val currency: String) : SettingsUiEvent
    data class ChangeLanguage(val language: String) : SettingsUiEvent
    data class ChangeThemeMode(val themeMode: String) : SettingsUiEvent
    data class ChangeCalendar(val calendar: String) : SettingsUiEvent
    data class ToggleAutoSave(val enabled: Boolean) : SettingsUiEvent
    data class ToggleBankNotifications(val enabled: Boolean) : SettingsUiEvent
    data class ToggleBankNotificationsSound(val enabled: Boolean) : SettingsUiEvent
    data class ToggleBankNotificationsShowAmount(val show: Boolean) : SettingsUiEvent
    data class ToggleBankNotificationsHideLockscreen(val hide: Boolean) : SettingsUiEvent
    data class ToggleBankNotificationsAutoSuggestCategory(val suggest: Boolean) : SettingsUiEvent
    data class ToggleBankNotificationsAutoSuggestAccount(val suggest: Boolean) : SettingsUiEvent
    data object ClearPendingDrafts : SettingsUiEvent

    /** Shows a transient message (snackbar) without changing any setting. */
    data class NotifyUnavailable(val message: String) : SettingsUiEvent
}
