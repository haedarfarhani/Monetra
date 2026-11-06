package ir.heydar.monetra.feature.smsimport.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a message may be treated as a bank transaction. OTP/authentication and
 * promotional messages are rejected outright and are never stored.
 */
@Singleton
class TransactionMessageClassifier @Inject constructor() {

    private val sensitiveMarkers = listOf(
        "رمز یکبار مصرف", "رمز یکبارمصرف", "رمز پویا", "کد تایید", "کد تأیید", "کد ورود",
        "otp", "پین", "password", "رمز عبور", "شماره رمز", "ورود به سیستم", "هشدار ورود",
        "فعالسازی", "کد فعال سازی", "کد فعال‌سازی"
    )

    private val promotionalMarkers = listOf(
        "برنده شوید", "جایزه", "قرعه کشی", "قرعه‌کشی", "جشنواره", "وام بدون ضامن",
        "تسهیلات ویژه", "افتتاح حساب آنلاین", "خرید اقساطی", "تخفیف ویژه"
    )

    private val financialMarkers = listOf(
        "برداشت", "واریز", "خرید", "پرداخت", "انتقال", "حواله", "موجودی", "باقیمانده",
        "مانده", "بستانکار", "بدهکار", "کارت", "حساب", "سپرده", "اصلاحیه", "کسر"
    )

    private val amountMarkerWithUnit = Regex("""[0-9][0-9,]*\s*(ریال|ريال|تومان|دلار)""")
    private val amountMarkerWithLabel = Regex("""(?:مبلغ|مانده|موجودی)[:\s]*([0-9][0-9,]*)""")

    fun isFinancialTransaction(normalizedMessage: String): Boolean {
        val lower = normalizedMessage.lowercase()
        if (sensitiveMarkers.any { lower.contains(it) }) return false
        if (promotionalMarkers.any { lower.contains(it) }) return false
        if (!financialMarkers.any { lower.contains(it) }) return false

        val hasAmountWithUnit = amountMarkerWithUnit.containsMatchIn(normalizedMessage)
        val hasAmountWithLabel = amountMarkerWithLabel.containsMatchIn(normalizedMessage)

        return hasAmountWithUnit || hasAmountWithLabel
    }
}
