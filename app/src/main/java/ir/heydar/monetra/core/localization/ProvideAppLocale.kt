package ir.heydar.monetra.core.localization

import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * Applies [languageCode] to the whole Compose tree.
 *
 * `stringResource` resolves the string table from `LocalResources`, not from
 * `LocalConfiguration`, so overriding the configuration alone leaves every label on the system
 * locale. `LocalContext` is intentionally left untouched: it has to stay the Activity, otherwise
 * `hiltViewModel()` and other Activity-scoped Compose APIs break.
 */
@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val locale = remember(languageCode) { resolveLocale(languageCode) }

    val localizedResources = remember(baseContext, baseConfiguration, locale) {
        val configuration = Configuration(baseConfiguration).apply { setLocale(locale) }
        baseContext.createConfigurationContext(configuration).resources
    }

    val layoutDirection = remember(locale) { layoutDirectionOf(locale) }

    CompositionLocalProvider(
        LocalResources provides localizedResources,
        LocalConfiguration provides localizedResources.configuration,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}

private fun resolveLocale(languageCode: String): Locale = when {
    languageCode.isEmpty() -> Locale.getDefault()
    languageCode.startsWith("fa") -> Locale.forLanguageTag("fa-IR")
    else -> Locale.forLanguageTag(languageCode)
}

private fun layoutDirectionOf(locale: Locale): LayoutDirection =
    if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
