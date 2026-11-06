package ir.heydar.monetra.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `converts persian digits to latin`() {
        assertEquals("1234567890", TextNormalizer.normalize("۱۲۳۴۵۶۷۸۹۰"))
    }

    @Test
    fun `converts arabic digits to latin`() {
        assertEquals("1234567890", TextNormalizer.normalize("١٢٣٤٥٦٧٨٩٠"))
    }

    @Test
    fun `replaces arabic yeh and kaf with persian letters`() {
        assertEquals("علی کریم", TextNormalizer.normalize("علي كريم"))
    }

    @Test
    fun `normalizes thousand separators and whitespace`() {
        assertEquals("1,500,000 ریال", TextNormalizer.normalize("۱٬۵۰۰٬۰۰۰   ریال"))
    }

    @Test
    fun `treats zero width non joiner as a space`() {
        assertEquals("می شود", TextNormalizer.normalize("می\u200Cشود"))
    }

    @Test
    fun `digit extraction keeps only numbers`() {
        assertEquals("61041234", TextNormalizer.digitsOf("6104-****-1234"))
    }
}
