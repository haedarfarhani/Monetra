package ir.heydar.monetra.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CURRENCY = stringPreferencesKey("currency")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CALENDAR_SYSTEM = stringPreferencesKey("calendar_system")
        val AUTO_SAVE_BANK_MESSAGES = booleanPreferencesKey("auto_save_bank_messages")
        val BANK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("bank_notifications_enabled")
        val BANK_NOTIFICATIONS_SOUND = booleanPreferencesKey("bank_notifications_sound")
        val BANK_NOTIFICATIONS_SHOW_AMOUNT = booleanPreferencesKey("bank_notifications_show_amount")
        val BANK_NOTIFICATIONS_HIDE_LOCKSCREEN = booleanPreferencesKey("bank_notifications_hide_lockscreen")
        val BANK_NOTIFICATIONS_AUTO_SUGGEST_CATEGORY = booleanPreferencesKey("bank_notifications_auto_suggest_category")
        val BANK_NOTIFICATIONS_AUTO_SUGGEST_ACCOUNT = booleanPreferencesKey("bank_notifications_auto_suggest_account")
        val PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("permission_prompt_shown")
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    /** Default currency for new accounts and transactions. Never rewrites existing amounts. */
    val currency: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY] ?: "USD"
    }

    suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: ""
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    /** Empty string means "follow the system" — see [ir.heydar.monetra.core.designsystem.theme.ThemeMode]. */
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: ""
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    /** See [ir.heydar.monetra.core.calendar.CalendarSystem]; affects display only. */
    val calendarSystem: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CALENDAR_SYSTEM] ?: "gregorian"
    }

    suspend fun setCalendarSystem(calendarSystem: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_SYSTEM] = calendarSystem
        }
    }

    /** Off by default: uncertain messages must always be reviewed by the user. */
    val autoSaveBankMessages: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SAVE_BANK_MESSAGES] ?: false
    }

    suspend fun setAutoSaveBankMessages(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_BANK_MESSAGES] = enabled
        }
    }

    val bankNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setBankNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val bankNotificationsSound: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_SOUND] ?: true
    }

    suspend fun setBankNotificationsSound(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_SOUND] = enabled
        }
    }

    val bankNotificationsShowAmount: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_SHOW_AMOUNT] ?: true
    }

    suspend fun setBankNotificationsShowAmount(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_SHOW_AMOUNT] = show
        }
    }

    val bankNotificationsHideLockscreen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_HIDE_LOCKSCREEN] ?: false
    }

    suspend fun setBankNotificationsHideLockscreen(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_HIDE_LOCKSCREEN] = hide
        }
    }

    val bankNotificationsAutoSuggestCategory: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_AUTO_SUGGEST_CATEGORY] ?: true
    }

    suspend fun setBankNotificationsAutoSuggestCategory(suggest: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_AUTO_SUGGEST_CATEGORY] = suggest
        }
    }

    val bankNotificationsAutoSuggestAccount: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BANK_NOTIFICATIONS_AUTO_SUGGEST_ACCOUNT] ?: true
    }

    suspend fun setBankNotificationsAutoSuggestAccount(suggest: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANK_NOTIFICATIONS_AUTO_SUGGEST_ACCOUNT] = suggest
        }
    }

    /** True once the runtime permission dialogs were raised, so they are asked only once at launch. */
    val permissionPromptShown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PERMISSION_PROMPT_SHOWN] ?: false
    }

    suspend fun setPermissionPromptShown() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERMISSION_PROMPT_SHOWN] = true
        }
    }
}
