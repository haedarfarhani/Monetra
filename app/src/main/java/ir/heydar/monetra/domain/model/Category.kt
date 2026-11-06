package ir.heydar.monetra.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val icon: String = "",
    val color: String = "#FF9800",
    val isSystem: Boolean = false
)

