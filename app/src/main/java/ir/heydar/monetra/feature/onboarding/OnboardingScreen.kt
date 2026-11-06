package ir.heydar.monetra.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.AppDateFormatter
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.AppOutlinedButton
import ir.heydar.monetra.core.designsystem.components.AppPrimaryButton
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.ThemeMode
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.formatting.MoneyFormatter
import java.util.Locale

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onFinished()
    }

    OnboardingScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingMd),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                StepIndicator(step = uiState.step)
                Spacer(modifier = Modifier.height(24.dp))

                when (uiState.step) {
                    OnboardingUiState.STEP_WELCOME -> WelcomeStep()

                    OnboardingUiState.STEP_LANGUAGE -> LanguageStep(
                        selectedCode = uiState.languageCode,
                        onSelect = { onEvent(OnboardingUiEvent.SelectLanguage(it)) }
                    )

                    OnboardingUiState.STEP_CALENDAR -> CalendarStep(
                        selected = uiState.calendarSystem,
                        languageCode = uiState.languageCode,
                        onSelect = { onEvent(OnboardingUiEvent.SelectCalendar(it)) }
                    )

                    OnboardingUiState.STEP_CURRENCY -> CurrencyStep(
                        selected = uiState.currency,
                        languageCode = uiState.languageCode,
                        onSelect = { onEvent(OnboardingUiEvent.SelectCurrency(it)) }
                    )

                    OnboardingUiState.STEP_THEME -> ThemeStep(
                        selectedTheme = uiState.themeMode,
                        onSelect = { onEvent(OnboardingUiEvent.SelectTheme(it)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.spacingMd),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.step > OnboardingUiState.STEP_WELCOME) {
                    AppOutlinedButton(
                        text = stringResource(id = R.string.action_back),
                        onClick = { onEvent(OnboardingUiEvent.Back) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    )
                }
                AppPrimaryButton(
                    text = if (uiState.isLastStep) {
                        stringResource(id = R.string.action_finish)
                    } else {
                        stringResource(id = R.string.action_next)
                    },
                    onClick = {
                        if (uiState.isLastStep) {
                            onEvent(OnboardingUiEvent.Finish)
                        } else {
                            onEvent(OnboardingUiEvent.Next)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving,
                    isLoading = uiState.isSaving
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(OnboardingUiState.STEP_COUNT) { index ->
            val isCurrentOrPast = index <= step
            val color by animateColorAsState(
                targetValue = if (isCurrentOrPast) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                label = "indicatorColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = stringResource(
            id = R.string.onboarding_step,
            step + 1,
            OnboardingUiState.STEP_COUNT
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WelcomeStep() {
    val extendedColors = MaterialTheme.monetraColors
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            extendedColors.balanceGradientStart,
            extendedColors.balanceGradientEnd
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Hero Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(id = R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.onboarding_welcome_tagline),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun LanguageStep(selectedCode: String, onSelect: (String) -> Unit) {
    StepHeader(
        title = stringResource(id = R.string.select_language_title),
        subtitle = stringResource(id = R.string.select_language_subtitle)
    )

    OptionCard(
        title = stringResource(id = R.string.language_persian),
        supporting = "فارسی (ایران)",
        selected = selectedCode == "fa",
        onClick = { onSelect("fa") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    OptionCard(
        title = stringResource(id = R.string.language_english),
        supporting = "English (US)",
        selected = selectedCode == "en",
        onClick = { onSelect("en") }
    )
}

@Composable
private fun CalendarStep(
    selected: CalendarSystem,
    languageCode: String,
    onSelect: (CalendarSystem) -> Unit
) {
    StepHeader(
        title = stringResource(id = R.string.calendar_title),
        subtitle = stringResource(id = R.string.calendar_subtitle)
    )

    val sampleLocale = remember(languageCode) { Locale.forLanguageTag(languageCode) }
    val today = remember { System.currentTimeMillis() }

    CalendarSystem.entries.forEachIndexed { index, system ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        OptionCard(
            title = stringResource(id = calendarLabelRes(system)),
            supporting = AppDateFormatter.formatDate(today, system, sampleLocale),
            selected = selected == system,
            onClick = { onSelect(system) }
        )
    }
}

@Composable
private fun CurrencyStep(
    selected: AppCurrency,
    languageCode: String,
    onSelect: (AppCurrency) -> Unit
) {
    StepHeader(
        title = stringResource(id = R.string.currency_title),
        subtitle = stringResource(id = R.string.currency_subtitle)
    )

    val sampleLocale = remember(languageCode) { Locale.forLanguageTag(languageCode) }

    AppCurrency.entries.forEachIndexed { index, currency ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        OptionCard(
            title = stringResource(id = currencyLabelRes(currency)),
            supporting = MoneyFormatter.format(sampleAmount(currency), currency, sampleLocale),
            selected = selected == currency,
            onClick = { onSelect(currency) }
        )
    }
}

@Composable
private fun ThemeStep(
    selectedTheme: String,
    onSelect: (String) -> Unit
) {
    StepHeader(
        title = stringResource(id = R.string.onboarding_theme_title),
        subtitle = stringResource(id = R.string.onboarding_theme_subtitle)
    )

    val themeOptions = listOf(
        Triple(ThemeMode.SYSTEM.key, stringResource(id = R.string.theme_system), Icons.Outlined.SettingsBrightness),
        Triple(ThemeMode.LIGHT.key, stringResource(id = R.string.theme_light), Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK.key, stringResource(id = R.string.theme_dark), Icons.Outlined.DarkMode)
    )

    themeOptions.forEachIndexed { index, (key, title, icon) ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        OptionCard(
            title = title,
            supporting = null,
            icon = icon,
            selected = selectedTheme == key,
            onClick = { onSelect(key) }
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun OptionCard(
    title: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val shape: Shape = MaterialTheme.shapes.medium
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.border(
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        ),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        onClick = onClick,
        fill = if (selected) {
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (supporting != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun calendarLabelRes(system: CalendarSystem): Int = when (system) {
    CalendarSystem.JALALI -> R.string.calendar_jalali
    CalendarSystem.GREGORIAN -> R.string.calendar_gregorian
}

private fun currencyLabelRes(currency: AppCurrency): Int = when (currency) {
    AppCurrency.USD -> R.string.currency_usd
    AppCurrency.IRR -> R.string.currency_irr
    AppCurrency.TOMAN -> R.string.currency_toman
}

private fun sampleAmount(currency: AppCurrency): Long = when (currency) {
    AppCurrency.USD -> 1_234_56L
    AppCurrency.IRR -> 12_345_678L
    AppCurrency.TOMAN -> 1_234_567L
}
