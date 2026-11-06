package ir.heydar.monetra.domain.model

/**
 * Processing status for an incoming bank SMS message.
 */
enum class SmsProcessingStatus {
    SUCCESS,
    PARTIAL,
    UNKNOWN_FORMAT,
    INVALID_TRANSACTION,
    UNSUPPORTED_BANK,
    DUPLICATE
}
