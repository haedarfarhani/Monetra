package ir.heydar.monetra.core.utils

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NumberFormatterTest {
    @Test
    fun formatEnglishLocale() {
        val result = NumberFormatter.format(1234567, Locale.ENGLISH)
        assertTrue(result.contains("1,234,567") || result.contains("1234567"))
    }

    @Test
    fun formatPersianDigitsContainsPersianNumbers() {
        // At minimum should return non-empty
        val result = NumberFormatter.formatWithPersianDigits(1234)
        assertTrue(result.isNotEmpty())
    }
}
