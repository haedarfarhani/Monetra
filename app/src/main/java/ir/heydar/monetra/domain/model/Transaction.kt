package ir.heydar.monetra.domain.model

/**
 * A transaction's amount is always stored in minor units of [currencyCode]: cents for USD and
 * whole units for IRR/TOMAN. `amountMinorUnits` of 1234 with `USD` means 12.34 dollars.
 */
data class Transaction(
    val id: Long = 0L,
    val accountId: Long,
    val counterAccountId: Long? = null,
    val categoryId: Long? = null,
    val amountMinorUnits: Long,
    val currencyCode: String = "USD",
    val type: TransactionType,
    val description: String? = null,
    val notes: String? = null,
    val referenceNumber: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val bankName: String? = null,
    val transferGroupId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
