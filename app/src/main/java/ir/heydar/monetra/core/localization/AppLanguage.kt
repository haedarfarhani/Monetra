package ir.heydar.monetra.core.localization

import java.util.Locale

/**
 * Supported application languages.
 * Persists explicit user selection; fallback to system default when not set.
 */
enum class AppLanguage(val code: String, val displayCode: String, val localeCode: Locale?) {
    SYSTEM(code = "", displayCode = "System Default", localeCode = null),
    ENGLISH(code = "en", displayCode = "English", localeCode = Locale.forLanguageTag("en")),
    PERSIAN(code = "fa", displayCode = "فارسی", localeCode = Locale.forLanguageTag("fa-IR"))
}

const val DEFAULT_LANGUAGE_CODE = "en"

fun AppLanguage.toLocale(): Locale {
    val code = this.code
    return if (code.isEmpty()) {
        Locale.getDefault()
    } else {
        this.localeCode ?: Locale.forLanguageTag(code)
    }
}
