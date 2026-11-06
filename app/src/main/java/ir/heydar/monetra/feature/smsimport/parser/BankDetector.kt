package ir.heydar.monetra.feature.smsimport.parser

import ir.heydar.monetra.core.text.TextNormalizer
import ir.heydar.monetra.domain.model.BankInfo
import javax.inject.Inject
import javax.inject.Singleton

enum class DetectionConfidence {
    HIGH,
    MEDIUM,
    NONE
}

data class BankDetectionResult(
    val bankInfo: BankInfo?,
    val confidence: DetectionConfidence
)

@Singleton
class BankDetector @Inject constructor() {

    val supportedBanks: List<BankInfo> = listOf(
        BankInfo("MELLAT", "Mellat", "بانک ملت", listOf("9830002222", "20004000", "MELLAT", "30002222", "ملت", "Mellat", "mellat")),
        BankInfo("MELLI", "Melli", "بانک ملی", listOf("9830001111", "30001111", "MELLI", "ملی", "Melli", "melli", "Bank Melli")),
        BankInfo("SADERAT", "Saderat", "بانک صادرات", listOf("9830003333", "30003333", "SADERAT", "صادرات", "Saderat", "saderat", "Sadat")),
        BankInfo("TEJARAT", "Tejarat", "بانک تجارت", listOf("9830004444", "30004444", "TEJARAT", "تجارت", "Tejarat", "tejarat")),
        BankInfo("SEPAH", "Sepah", "بانک سپه", listOf("9830005555", "30005555", "SEPAH", "سپه", "Sepah", "sepah")),
        BankInfo("PASARGAD", "Pasargad", "بانک پاسارگاد", listOf("9830006666", "30006666", "PASARGAD", "BPI", "پاسارگاد", "Pasargad", "pasargad")),
        BankInfo("SAMAN", "Saman", "بانک سامان", listOf("9830007777", "30007777", "SAMAN", "سامان", "Saman", "saman")),
        BankInfo("PARSIAN", "Parsian", "بانک پارسیان", listOf("9830008888", "30008888", "PARSIAN", "پارس", "Parsian", "parsian")),
        BankInfo("EGHTESAD_NOVIN", "Eghtesad Novin", "بانک اقتصاد نوین", listOf("EGHTESADNOVIN", "ENBANK", "اقتصاد نوین", "Eghtesad Novin", "eghtesad")),
        BankInfo("SHAHR", "Shahr", "بانک شهر", listOf("SHAHR", "CITYBANK", "شهر", "Shahr", "shahr", "City Bank")),
        BankInfo("REFAH", "Refah", "بانک رفاه", listOf("REFAH", "REFAHBANK", "رفاه", "Refah", "refah")),
        BankInfo("KESHAVARZI", "Keshavarzi", "بانک کشاورزی", listOf("KESHAVARZI", "AGRIBANK", "کشاورزی", "Keshavarzi", "keshavarzi", "AgriBank")),
        BankInfo("MASKAN", "Maskan", "بانک مسکن", listOf("MASKAN", "MASKANBANK", "مسکن", "Maskan", "maskan", "Housing")),
        BankInfo("POST_BANK", "Post Bank", "پست بانک", listOf("POSTBANK", "پست", "Post", "post bank")),
        BankInfo("AYANDEH", "Ayandeh", "بانک آینده", listOf("AYANDEH", "AYANDEHBANK", "آینده", "Ayandeh", "ayandeh"))
    )

    fun detect(sender: String?, normalizedMessage: String): BankDetectionResult {
        // Step 1: Try content-based detection FIRST (more robust than sender numbers)
        // This ensures messages work even when sender number changes over time
        for (bank in supportedBanks) {
            if (normalizedMessage.contains(bank.nameFa)) {
                return BankDetectionResult(bank, DetectionConfidence.HIGH)
            }
            if (normalizedMessage.contains(bank.nameEn, ignoreCase = true)) {
                return BankDetectionResult(bank, DetectionConfidence.HIGH)
            }
            for (alias in bank.senderAliases) {
                if (normalizedMessage.contains(alias, ignoreCase = true)) {
                    return BankDetectionResult(bank, DetectionConfidence.HIGH)
                }
            }
        }

        // Step 2: Try short keywords (medium confidence) - fallback for partial matches
        for (bank in supportedBanks) {
            val shortKeyword = bank.nameFa.removePrefix("بانک").trim()
            if (shortKeyword.length >= 3 && normalizedMessage.contains(shortKeyword)) {
                return BankDetectionResult(bank, DetectionConfidence.MEDIUM)
            }
        }

        // Step 3: Try sender-based detection (lower priority, for when content doesn't match)
        val cleanSender = sender?.uppercase()?.trim().orEmpty()
        val senderDigits = TextNormalizer.digitsOf(cleanSender)

        if (cleanSender.isNotEmpty()) {
            for (bank in supportedBanks) {
                if (bank.senderAliases.any { alias ->
                        cleanSender.contains(alias.uppercase()) ||
                            (senderDigits.isNotEmpty() && senderDigits.endsWith(TextNormalizer.digitsOf(alias)))
                }) {
                    return BankDetectionResult(bank, DetectionConfidence.HIGH)
                }
            }
        }

        return BankDetectionResult(null, DetectionConfidence.NONE)
    }
}
