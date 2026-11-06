package ir.heydar.monetra.domain.model

/**
 * Representation of a supported bank in Monetra.
 */
data class BankInfo(
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val senderAliases: List<String> = emptyList(),
    val iconResId: Int? = null
)
