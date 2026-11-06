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

    private val bankDetector = BankDetector()
    private val registry = BankMessageParserRegistry(
        parsers = setOf(
            MelliBankParser(),
            MellatBankParser(),
            SaderatBankParser(),
            TejaratBankParser(),
            SepahBankParser(),
            PasargadBankParser(),
            SamanBankParser(),
            ParsianBankParser(),
            GenericBankParser()
        ),
        normalizer = BankMessageNormalizer(),
        classifier = TransactionMessageClassifier(),
        senderResolver = BankSenderResolver(bankDetector),
        bankDetector = bankDetector
    )

    @Test
    fun `parses compact user bank sms message format`() {
        val rawMessage = "حساب5630859166\nبرداشت5,000\nمانده241,252\n05/08/14-19:52"
        val result = registry.parseSmsResult(rawMessage)

        val draft = result.draft
        assertNotNull(draft)
        assertEquals(TransactionType.EXPENSE, draft?.transactionType)
        assertEquals(5_000L, draft?.amountMinorUnits)
        assertEquals(241_252L, draft?.accountBalanceMinorUnits)
        assertEquals("5630859166", draft?.accountNumber)
    }

    @Test
    fun `parses bank melli withdrawal with persian digits`() {
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
    fun `parses bank mellat card transfer`() {
        val draft = registry.parse(
            "بانک ملت: انتقال از کارت 6104-****-****-1234 مبلغ 250,000 ریال"
        )

        assertEquals("Mellat", draft.bankName)
        assertEquals(TransactionType.TRANSFER, draft.transactionType)
        assertEquals(250_000L, draft.amountMinorUnits)
        assertEquals("1234", draft.cardLastFourDigits)
    }

    @Test
    fun `parses bank mellat withdrawal without unit when sender provided`() {
        val draft = registry.parse(
            "برداشت از حساب\nمبلغ: ۱۰۵,۰۰۰\nمانده: ۴,۲۳۶,۲۵۲",
            sender = "Mellat"
        )

        assertEquals("Mellat", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(105_000L, draft.amountMinorUnits)
        assertEquals(4_236_252L, draft.accountBalanceMinorUnits)
    }

    @Test
    fun `parses bank mellat deposit without unit when sender provided`() {
        val draft = registry.parse(
            "واریز به حساب\nمبلغ: ۵,۰۰۰,۰۰۰\nمانده: ۵,۲۴۱,۲۵۲",
            sender = "9830002222"
        )

        assertEquals("Mellat", draft.bankName)
        assertEquals(TransactionType.INCOME, draft.transactionType)
        assertEquals(5_000_000L, draft.amountMinorUnits)
        assertEquals(5_241_252L, draft.accountBalanceMinorUnits)
    }

    @Test
    fun `parses bank mellat purchase`() {
        val draft = registry.parse(
            "بانک ملت\nخرید\nمبلغ: 120,000 ریال"
        )

        assertEquals("Mellat", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(120_000L, draft.amountMinorUnits)
    }

    @Test
    fun `parses bank tejarat deposit in toman`() {
        val draft = registry.parse(
            "بانک تجارت: واریز به حساب 1234567890 مبلغ ۹۰۰,۰۰۰ تومان"
        )

        assertEquals("Tejarat", draft.bankName)
        assertEquals(TransactionType.INCOME, draft.transactionType)
        assertEquals(900_000L, draft.amountMinorUnits)
        assertEquals(AppCurrency.TOMAN.code, draft.currencyCode)
    }

    @Test
    fun `parses bank saderat card purchase`() {
        val draft = registry.parse(
            "بانک صادرات: خرید از کارت 6037****1234 مبلغ 350,000 ریال"
        )

        assertEquals("Saderat", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(350_000L, draft.amountMinorUnits)
    }

    @Test
    fun `parses bank sepah withdrawal`() {
        val draft = registry.parse(
            "بانک سپه: کسر از حساب 1234567 مبلغ 500,000 ریال مانده 1,200,000 ریال"
        )

        assertEquals("Sepah", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(500_000L, draft.amountMinorUnits)
    }

    @Test
    fun `parses bank pasargad withdrawal`() {
        val draft = registry.parse(
            "بانک پاسارگاد: برداشت از کارت 5022****1234 مبلغ 750,000 ریال موجودی 3,000,000 ریال"
        )

        assertEquals("Pasargad", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(750_000L, draft.amountMinorUnits)
    }

    @Test
    fun `parses bank saman deposit`() {
        val draft = registry.parse(
            "بانک سامان: واریز به حساب 800-123-45678-1 مبلغ 1,200,000 ریال"
        )

        assertEquals("Saman", draft.bankName)
        assertEquals(TransactionType.INCOME, draft.transactionType)
        assertEquals(1_200_000L, draft.amountMinorUnits)
    }

    @Test
    fun `parses bank parsian purchase`() {
        val draft = registry.parse(
            "بانک پارسیان: خرید کارت 6221****1234 مبلغ 150,000 ریال"
        )

        assertEquals("Parsian", draft.bankName)
        assertEquals(TransactionType.EXPENSE, draft.transactionType)
        assertEquals(150_000L, draft.amountMinorUnits)
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
