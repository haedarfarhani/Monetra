package ir.heydar.monetra.domain.model

/** Query parameters for the transaction history screen. */
data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val startMillis: Long = 0L,
    val endMillis: Long = Long.MAX_VALUE,
    val limit: Int = 50,
    val offset: Int = 0
)
