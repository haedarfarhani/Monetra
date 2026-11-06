package ir.heydar.monetra.core.formatting

import ir.heydar.monetra.core.currency.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {

    private val us = Locale.US

    @Test
    fun `formats dollars with two fraction digits`() {
        assertEquals("1,234.56 $", MoneyFormatter.format(123_456L, AppCurrency.USD, us))
    }

    @Test
    fun `formats rials without fraction digits`() {
        assertEquals("123,456 ریال", MoneyFormatter.format(123_456L, AppCurrency.IRR, us))
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("-1,234.56 $", MoneyFormatter.format(-123_456L, AppCurrency.USD, us))
    }

    @Test
    fun `parses major units into minor units`() {
        assertEquals(123_456L, MoneyFormatter.parseToMinorUnits("1234.56", AppCurrency.USD))
        assertEquals(123_400L, MoneyFormatter.parseToMinorUnits("1234", AppCurrency.USD))
        assertEquals(1_234L, MoneyFormatter.parseToMinorUnits("1234", AppCurrency.IRR))
    }

    @Test
    fun `rejects input that does not fit the currency precision`() {
        assertNull(MoneyFormatter.parseToMinorUnits("", AppCurrency.USD))
        assertNull(MoneyFormatter.parseToMinorUnits("12.345", AppCurrency.USD))
        assertNull(MoneyFormatter.parseToMinorUnits("12.3", AppCurrency.IRR))
        assertNull(MoneyFormatter.parseToMinorUnits("abc", AppCurrency.USD))
        assertNull(MoneyFormatter.parseToMinorUnits("1.2.3", AppCurrency.USD))
    }
}
