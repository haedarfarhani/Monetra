package ir.heydar.monetra.core.common

/**
 * Custom exceptions used across the personal finance app.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class DatabaseException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class NotFoundException(
        override val message: String
    ) : AppException(message)

    data class ValidationException(
        override val message: String
    ) : AppException(message)

    data class SecurityException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class UnknownException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException(message, cause)
}

