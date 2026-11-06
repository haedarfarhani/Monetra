package ir.heydar.monetra.feature.onboarding

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.theme.ThemeMode

data class OnboardingUiState(
    val step: Int = STEP_WELCOME,
    val languageCode: String = "en",
    val calendarSystem: CalendarSystem = CalendarSystem.GREGORIAN,
    val currency: AppCurrency = AppCurrency.USD,
    val themeMode: String = ThemeMode.SYSTEM.key,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false
) {
    val isLastStep: Boolean get() = step == STEP_COUNT - 1

    companion object {
        const val STEP_WELCOME = 0
        const val STEP_LANGUAGE = 1
        const val STEP_CALENDAR = 2
        const val STEP_CURRENCY = 3
        const val STEP_THEME = 4
        const val STEP_COUNT = 5
    }
}
