package ir.heydar.monetra.core.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun systemDefaultCodeIsEmpty() {
        assertEquals("", AppLanguage.SYSTEM.code)
    }

    @Test
    fun persianLocaleIsFa() {
        assertEquals("fa", AppLanguage.PERSIAN.code)
    }
}
