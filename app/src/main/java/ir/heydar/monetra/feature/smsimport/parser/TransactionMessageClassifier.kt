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
        "otp", "پین", "password", "رمز عبور", "شماره رمز"
    )

    private val financialMarkers = listOf(
        "برداشت", "واریز", "خرید", "پرداخت", "انتقال", "حواله", "موجودی", "باقیمانده",
        "بستانکار", "بدهکار", "کارت", "حساب"
    )

    private val amountMarker = Regex("""[0-9][0-9,]*\s*(ریال|تومان|دلار)""")

    fun isFinancialTransaction(normalizedMessage: String): Boolean {
        val lower = normalizedMessage.lowercase()
        if (sensitiveMarkers.any { lower.contains(it) }) return false
        if (!financialMarkers.any { lower.contains(it) }) return false
        return amountMarker.containsMatchIn(normalizedMessage)
    }
}
