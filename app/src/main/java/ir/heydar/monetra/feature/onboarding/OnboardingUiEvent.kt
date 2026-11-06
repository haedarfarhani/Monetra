package ir.heydar.monetra.feature.onboarding

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency

sealed interface OnboardingUiEvent {
    data class SelectLanguage(val code: String) : OnboardingUiEvent
    data class SelectCalendar(val calendarSystem: CalendarSystem) : OnboardingUiEvent
    data class SelectCurrency(val currency: AppCurrency) : OnboardingUiEvent
    data class SelectTheme(val themeMode: String) : OnboardingUiEvent
    data object Next : OnboardingUiEvent
    data object Back : OnboardingUiEvent
    data object Finish : OnboardingUiEvent
}
