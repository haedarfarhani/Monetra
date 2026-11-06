package ir.heydar.monetra.feature.smsimport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries text shared through the Android sharesheet into the import screen without putting it in
 * the navigation route (bank messages are long and sensitive).
 */
@Singleton
class SharedTextStore @Inject constructor() {

    private val _text = MutableStateFlow<String?>(null)
    val text: StateFlow<String?> = _text.asStateFlow()

    fun offer(value: String?) {
        if (!value.isNullOrBlank()) _text.value = value
    }

    fun consume(): String? {
        val current = _text.value
        _text.value = null
        return current
    }
}
