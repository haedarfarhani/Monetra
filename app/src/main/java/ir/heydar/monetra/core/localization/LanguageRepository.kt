package ir.heydar.monetra.core.localization

import ir.heydar.monetra.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    val selectedLanguageCode: Flow<String> = preferencesManager.language
    val isFirstLaunch: Flow<Boolean> = preferencesManager.isFirstLaunch

    suspend fun setLanguage(code: String) {
        preferencesManager.setLanguage(code)
    }

    suspend fun completeFirstLaunch() {
        preferencesManager.setFirstLaunchCompleted()
    }

    fun isSystemDefault(): Flow<Boolean> = selectedLanguageCode.map { it.isEmpty() }
}
