package ir.heydar.monetra.feature.settings

sealed interface SettingsUiEffect {
    data object DraftsCleared : SettingsUiEffect
    data class ShowMessage(val message: String) : SettingsUiEffect
}
