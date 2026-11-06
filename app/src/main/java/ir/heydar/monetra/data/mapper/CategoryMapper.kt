package ir.heydar.monetra.data.mapper

import ir.heydar.monetra.data.local.entity.CategoryEntity
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType

fun CategoryEntity.toDomain(): Category {
    val transactionType = try {
        TransactionType.valueOf(type)
    } catch (_: Exception) {
        TransactionType.EXPENSE
    }
    return Category(
        id = id,
        name = name,
        type = transactionType,
        icon = icon,
        color = color,
        isSystem = isSystem
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        type = type.name,
        icon = icon,
        color = color,
        isSystem = isSystem
    )
}

