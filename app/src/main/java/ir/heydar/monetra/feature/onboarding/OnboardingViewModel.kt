package ir.heydar.monetra.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.designsystem.theme.ThemeMode
import ir.heydar.monetra.core.localization.LanguageManager
import ir.heydar.monetra.core.localization.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val languageRepository: LanguageRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            step = OnboardingUiState.STEP_WELCOME,
            languageCode = if (isPersianDevice()) "fa" else "en",
            calendarSystem = if (isPersianDevice()) CalendarSystem.JALALI else CalendarSystem.GREGORIAN,
            currency = AppCurrency.USD,
            themeMode = ThemeMode.SYSTEM.key
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.SelectLanguage -> {
                _uiState.update { it.copy(languageCode = event.code) }
                viewModelScope.launch {
                    languageRepository.setLanguage(event.code)
                    languageManager.apply(event.code)
                }
            }

            is OnboardingUiEvent.SelectCalendar ->
                _uiState.update { it.copy(calendarSystem = event.calendarSystem) }

            is OnboardingUiEvent.SelectCurrency ->
                _uiState.update { it.copy(currency = event.currency) }

            is OnboardingUiEvent.SelectTheme ->
                _uiState.update { it.copy(themeMode = event.themeMode) }

            OnboardingUiEvent.Next -> _uiState.update {
                it.copy(step = (it.step + 1).coerceAtMost(OnboardingUiState.STEP_COUNT - 1))
            }

            OnboardingUiEvent.Back -> _uiState.update {
                it.copy(step = (it.step - 1).coerceAtLeast(0))
            }

            OnboardingUiEvent.Finish -> finish()
        }
    }

    private fun finish() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            languageRepository.setLanguage(state.languageCode)
            languageManager.apply(state.languageCode)
            preferencesManager.setCalendarSystem(state.calendarSystem.key)
            preferencesManager.setCurrency(state.currency.code)
            preferencesManager.setThemeMode(state.themeMode)
            languageRepository.completeFirstLaunch()
            _uiState.update { it.copy(isSaving = false, isCompleted = true) }
        }
    }

    private fun isPersianDevice(): Boolean =
        Locale.getDefault().language.equals("fa", ignoreCase = true)
}
