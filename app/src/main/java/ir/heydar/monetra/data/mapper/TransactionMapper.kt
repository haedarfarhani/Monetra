package ir.heydar.monetra.data.mapper

import ir.heydar.monetra.data.local.entity.TransactionEntity
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionSource
import ir.heydar.monetra.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction {
    val transactionType = try {
        TransactionType.valueOf(type)
    } catch (_: Exception) {
        TransactionType.EXPENSE
    }
    return Transaction(
        id = id,
        accountId = accountId,
        counterAccountId = counterAccountId,
        categoryId = categoryId,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        type = transactionType,
        description = description,
        notes = notes,
        referenceNumber = referenceNumber,
        source = TransactionSource.fromKey(source),
        bankName = bankName,
        transferGroupId = transferGroupId,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        accountId = accountId,
        counterAccountId = counterAccountId,
        categoryId = categoryId,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        type = type.name,
        description = description,
        notes = notes,
        referenceNumber = referenceNumber,
        source = source.name,
        bankName = bankName,
        transferGroupId = transferGroupId,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
