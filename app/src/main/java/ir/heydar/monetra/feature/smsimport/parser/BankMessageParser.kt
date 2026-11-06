package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.calendar.JalaliCalendar
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.BankTransactionType
import ir.heydar.monetra.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId

/** One bank's message format. Each implementation stays independent and small on purpose. */
interface BankMessageParser {
    val bankName: String

    /** The generic parser runs only when no bank specific parser matched. */
    val isFallback: Boolean get() = false

    fun matches(normalizedMessage: String, sender: String?): Boolean

    fun parse(normalizedMessage: String, sender: String?, rawMessage: String): BankTransactionDraft?
}

data class AmountMatch(
    val minorUnits: Long,
    val currency: AppCurrency,
    val raw: String
)

/** Regex helpers shared by the bank parsers. All input is expected to be normalized. */
object ParserSupport {

    private val amountWithUnit = Regex("""(?:مبلغ|واریز|برداشت|خرید|پرداخت|انتقال|کسر|بستانکار|بدهکار)[:\s]*([0-9][0-9,]*)\s*(ریال|ريال|تومان|دلار)?""")
    private val explicitUnitPattern = Regex("""([0-9][0-9,]*)\s*(ریال|ريال|تومان|دلار)""")
    private val bareAmountPattern = Regex("""مبلغ[:\s]*([0-9][0-9,]*)""")

    private val datePattern = Regex("""(13\d{2}|14\d{2}|20\d{2}|\d{2})[/-](\d{1,2})[/-](\d{1,2})""")
    private val timePattern = Regex("""(\d{1,2}):(\d{2})""")
    private val referencePattern = Regex("""(?:پیگیری|مرجع|رسید|سند|رهگیری|کد\s*رهگیری)[:\s]*([0-9A-Za-z]{4,})""")
    private val balancePattern = Regex("""(?:موجودی|باقیمانده|مانده|موجودیکارت)[:\s]*(?:مبلغ)?\s*([0-9][0-9,]*)""")
    private val maskedCardPattern = Regex("""\*{4}[\s-]*([0-9]{4})""")
    private val dashedCardPattern = Regex("""[0-9]{4}[\s-]+\*{4}[\s-]+\*{4}[\s-]+([0-9]{4})""")
    private val namedCardPattern = Regex("""(?:کارت|کارت\s*شماره)[^0-9]{0,12}([0-9]{4})(?![0-9])""")
    private val accountNumberPattern = Regex("""(?:حساب|سپرده)[:\s]*([0-9.\-/]{5,24})""")

    fun amount(normalizedMessage: String, anchor: String? = "مبلغ"): AmountMatch? {
        val anchored = anchor?.let { marker ->
            val index = normalizedMessage.indexOf(marker)
            if (index >= 0) normalizedMessage.substring(index) else null
        }

        val matchWithKeyword = amountWithUnit.find(anchored ?: normalizedMessage)
        if (matchWithKeyword != null) {
            val numStr = matchWithKeyword.groupValues[1]
            val unitStr = matchWithKeyword.groupValues[2].ifEmpty { null }
            val value = toLong(numStr)
            if (value != null && value > 0L) {
                val currency = unitStr?.let { currencyOf(it) }
                    ?: detectCurrencyFromMessage(normalizedMessage)
                    ?: AppCurrency.IRR
                return AmountMatch(toMinorUnits(value, currency), currency, matchWithKeyword.value)
            }
        }

        val explicitMatch = explicitUnitPattern.find(normalizedMessage)
        if (explicitMatch != null) {
            val value = toLong(explicitMatch.groupValues[1])
            if (value != null && value > 0L) {
                val currency = currencyOf(explicitMatch.groupValues[2])
                return AmountMatch(toMinorUnits(value, currency), currency, explicitMatch.value)
            }
        }

        val bareMatch = bareAmountPattern.find(normalizedMessage)
        if (bareMatch != null) {
            val value = toLong(bareMatch.groupValues[1])
            if (value != null && value > 0L) {
                val currency = detectCurrencyFromMessage(normalizedMessage) ?: AppCurrency.IRR
                return AmountMatch(toMinorUnits(value, currency), currency, bareMatch.value)
            }
        }

        return null
    }

    private fun detectCurrencyFromMessage(normalizedMessage: String): AppCurrency? {
        return when {
            normalizedMessage.contains("تومان") -> AppCurrency.TOMAN
            normalizedMessage.contains("دلار") -> AppCurrency.USD
            normalizedMessage.contains("ریال") || normalizedMessage.contains("ريال") -> AppCurrency.IRR
            else -> null
        }
    }

    fun currencyOf(word: String): AppCurrency = when (word) {
        "دلار" -> AppCurrency.USD
        "تومان" -> AppCurrency.TOMAN
        else -> AppCurrency.IRR
    }

    fun toMinorUnits(majorUnits: Long, currency: AppCurrency): Long =
        if (currency.fractionDigits == 0) majorUnits else majorUnits * 100

    fun transactionType(normalizedMessage: String): TransactionType? = when {
        containsAny(normalizedMessage, "انتقال", "حواله", "انتقالی", "پایا", "ساتنا", "کارت به کارت") -> TransactionType.TRANSFER
        containsAny(normalizedMessage, "برداشت", "خرید", "پرداخت", "کسر", "بدهکار", "کارمزد", "شماره کارت شما") ->
            TransactionType.EXPENSE
        containsAny(normalizedMessage, "واریز", "افزایش", "بستانکار", "واریزی", "برگشت", "اصلاحیه واریز") ->
            TransactionType.INCOME
        else -> null
    }

    fun bankTransactionType(normalizedMessage: String): BankTransactionType = when {
        containsAny(normalizedMessage, "اصلاحیه واریز", "برگشت") -> BankTransactionType.REVERSAL
        containsAny(normalizedMessage, "کارمزد") -> BankTransactionType.FEE
        containsAny(normalizedMessage, "کارت به کارت") -> BankTransactionType.CARD_TO_CARD
        containsAny(normalizedMessage, "انتقال", "حواله", "پایا", "ساتنا") ->
            if (containsAny(normalizedMessage, "به کارت", "به حساب", "واریز")) BankTransactionType.TRANSFER_IN
            else BankTransactionType.TRANSFER_OUT
        containsAny(normalizedMessage, "واریز", "افزایش", "بستانکار", "واریزی") -> BankTransactionType.INCOME
        containsAny(normalizedMessage, "برداشت", "خرید", "پرداخت", "کسر", "بدهکار") -> BankTransactionType.EXPENSE
        else -> BankTransactionType.UNKNOWN
    }

    fun timestamp(
        normalizedMessage: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val dateMatch = datePattern.find(normalizedMessage) ?: return null
        val rawYear = dateMatch.groupValues[1].toIntOrNull() ?: return null
        val month = dateMatch.groupValues[2].toIntOrNull() ?: return null
        val day = dateMatch.groupValues[3].toIntOrNull() ?: return null

        val year = if (rawYear < 100) 1400 + rawYear else rawYear

        val (gregorianYear, gregorianMonth, gregorianDay) = if (year in 1300..1499) {
            val converted = JalaliCalendar.toGregorian(year, month, day)
            Triple(converted[0], converted[1], converted[2])
        } else {
            Triple(year, month, day)
        }

        val timeMatch = timePattern.find(normalizedMessage)
        val hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minute = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

        return runCatching {
            LocalDate.of(gregorianYear, gregorianMonth, gregorianDay)
                .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    fun dateText(normalizedMessage: String): String? = datePattern.find(normalizedMessage)?.value

    fun timeText(normalizedMessage: String): String? = timePattern.find(normalizedMessage)?.value

    fun cardLastFour(normalizedMessage: String): String? =
        maskedCardPattern.find(normalizedMessage)?.groupValues?.get(1)
            ?: dashedCardPattern.find(normalizedMessage)?.groupValues?.get(1)
            ?: namedCardPattern.find(normalizedMessage)?.groupValues?.get(1)

    fun accountNumber(normalizedMessage: String): String? =
        accountNumberPattern.find(normalizedMessage)?.groupValues?.get(1)

    fun referenceNumber(normalizedMessage: String): String? =
        referencePattern.find(normalizedMessage)?.groupValues?.get(1)

    fun balance(normalizedMessage: String): Long? {
        val match = balancePattern.find(normalizedMessage) ?: return null
        return toLong(match.groupValues[1])
    }

    fun description(normalizedMessage: String): String? {
        val marker = listOf("بابت", "شرح", "به نام", "در وجه").firstOrNull { normalizedMessage.contains(it) }
            ?: return null
        val index = normalizedMessage.indexOf(marker)
        if (index < 0) return null
        return normalizedMessage.substring(index + marker.length).trim()
            .take(120)
            .ifBlank { null }
    }

    fun confidence(
        bankSpecific: Boolean,
        amount: AmountMatch?,
        type: TransactionType?,
        timestamp: Long?,
        reference: String?
    ): Int {
        var score = if (bankSpecific) 60 else 40
        if (amount != null) score += 15
        if (type != null) score += 10
        if (timestamp != null) score += 5
        if (reference != null) score += 5
        return score.coerceAtMost(95)
    }

    private fun containsAny(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it) }

    fun toLong(value: String): Long? =
        value.replace(",", "").replace("،", "").toLongOrNull()
}
