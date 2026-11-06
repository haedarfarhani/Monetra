package ir.heydar.monetra.domain.model

data class Account(
    val id: Long = 0L,
    val name: String,
    val type: String,
    val balance: Long,
    val initialBalance: Long = 0L,
    val currency: String = "USD",
    val icon: String = "",
    val color: String = "#4CAF50",
    val isDefault: Boolean = false,
    /** Optional role: "" none, "DAILY" daily spending, "SALARY" salary account. */
    val purpose: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val cardNumber: String = "",
    val holderName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PURPOSE_DAILY = "DAILY"
        const val PURPOSE_SALARY = "SALARY"
    }
}
