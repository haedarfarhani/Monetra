package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.text.MessageFingerprint
import ir.heydar.monetra.domain.model.ParsingStatus
import ir.heydar.monetra.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankMessageParsingTest {

    private val registry = BankMessageParserRegistry(
        parsers = setOf(
            MelliBankParser(),
            MellatBankParser(),
            SaderatBankParser(),
            TejaratBankParser(),
            GenericBankParser()
        ),
        normalizer = BankMessageNormalizer(),
        classifier = TransactionMessageClassifier(),
        senderResolver = BankSenderResolver()
    )

    @Test
    fun `parses a bank melli withdrawal with persian digits`() {
        val draft = registry.parse(
            "بانک ملی ایران: برداشت از حساب 1234567890 مبلغ ۱,۵۰۰,۰۰۰ ریال تاریخ ۱۴۰۳/۰۵/۱۲ ساعت ۱۴:۳۰"
        )

        assertEquals("Melli", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(1_500_000L, draft.amountMinorUnits)
        assertEquals(AppCurrency.IRR.code, draft.currencyCode)
        assertEquals(ParsingStatus.PARSED, draft.parsingStatus)
        assertTrue(draft.confidence >= 80)
        assertNotNull(draft.transactionTimestamp)
    }

    @Test
    fun `parses a bank mellat card transfer`() {
        val draft = registry.parse(
            "بانک ملت: انتقال از کارت 6104-****-****-1234 مبلغ 250,000 ریال"
        )

        assertEquals("Mellat", draft.bankName)
        assertEquals(TransactionType.TRANSFER, draft.transactionType)
        assertEquals(250_000L, draft.amountMinorUnits)
        assertEquals("1234", draft.cardLastFourDigits)
    }

    @Test
    fun `parses a bank tejarat deposit in toman`() {
        val draft = registry.parse(
            "بانک تجارت: واریز به حساب 1234567890 مبلغ ۹۰۰,۰۰۰ تومان"
        )

        assertEquals("Tejarat", draft.bankName)
        assertEquals(TransactionType.INCOME, draft.transactionType)
        assertEquals(900_000L, draft.amountMinorUnits)
        assertEquals(AppCurrency.TOMAN.code, draft.currencyCode)
    }

    @Test
    fun `parses a bank saderat card purchase`() {
        val draft = registry.parse(
            "بانک صادرات: خرید از کارت 6037****1234 مبلغ 350,000 ریال"
        )

        assertEquals("Saderat", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(350_000L, draft.amountMinorUnits)
    }

    @Test
    fun `generic parser never claims high confidence`() {
        val draft = registry.parse("حساب 1234567890 برداشت 700,000 ریال")

        assertNull(draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(ParsingStatus.NEEDS_REVIEW, draft.parsingStatus)
        assertTrue(draft.confidence < 80)
        assertTrue(!draft.isHighConfidence)
    }

    @Test
    fun `otp messages are rejected and never parsed`() {
        val draft = registry.parse("بانک ملی: رمز یکبار مصرف شما 123456")

        assertEquals(ParsingStatus.FAILED, draft.parsingStatus)
        assertNull(draft.amountMinorUnits)
    }

    @Test
    fun `duplicate messages share the same fingerprint`() {
        val message = "بانک ملت: خرید 120,000 ریال"
        val first = registry.parse(message)
        val second = registry.parse(message)

        assertEquals(first.fingerprint, second.fingerprint)
        assertEquals(first.fingerprint, MessageFingerprint.of(normalized(message)))
    }

    @Test
    fun `different messages have different fingerprints`() {
        val first = registry.parse("بانک ملت: خرید 120,000 ریال")
        val second = registry.parse("بانک ملت: خرید 121,000 ریال")

        assertNotEquals(first.fingerprint, second.fingerprint)
    }

    private fun normalized(message: String) = BankMessageNormalizer().normalize(message)
}
