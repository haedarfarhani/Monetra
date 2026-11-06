package ir.heydar.monetra.feature.settings

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val currency: String = "USD",
    val language: String = "en",
    val themeMode: String = "",
    val calendar: String = "gregorian",
    val autoSaveBankMessages: Boolean = false,
    val bankNotificationsEnabled: Boolean = true,
    val bankNotificationsSound: Boolean = true,
    val bankNotificationsShowAmount: Boolean = true,
    val bankNotificationsHideLockscreen: Boolean = false,
    val bankNotificationsAutoSuggestCategory: Boolean = true,
    val bankNotificationsAutoSuggestAccount: Boolean = true,
    val appVersion: String = ""
)
