package ir.heydar.monetra.core.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    private val repository: LanguageRepository,
    @ApplicationContext private val context: Context
) {

    init {
        val saved = runBlocking { repository.selectedLanguageCode.first() }
        apply(saved, context)
    }

    fun apply(code: String, context: Context = this.context) {
        val localeList = when {
            code.isEmpty() -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun observe(): Flow<String> = repository.selectedLanguageCode
}
