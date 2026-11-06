package ir.heydar.monetra.data.mapper

import ir.heydar.monetra.data.local.entity.AccountEntity
import ir.heydar.monetra.domain.model.Account

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = type,
        balance = balance,
        initialBalance = initialBalance,
        currency = currency,
        icon = icon,
        color = color,
        isDefault = isDefault,
        purpose = purpose,
        bankName = bankName,
        accountNumber = accountNumber,
        cardNumber = cardNumber,
        holderName = holderName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        type = type,
        balance = balance,
        initialBalance = initialBalance,
        currency = currency,
        icon = icon,
        color = color,
        isDefault = isDefault,
        purpose = purpose,
        bankName = bankName,
        accountNumber = accountNumber,
        cardNumber = cardNumber,
        holderName = holderName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
