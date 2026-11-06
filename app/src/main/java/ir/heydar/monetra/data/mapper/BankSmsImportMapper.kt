package ir.heydar.monetra.data.mapper

import ir.heydar.monetra.data.local.entity.BankSmsImportEntity
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.ParsingStatus

fun BankTransactionDraft.toImportEntity(status: ParsingStatus = parsingStatus): BankSmsImportEntity =
    BankSmsImportEntity(
        bankName = bankName,
        senderAddress = senderAddress,
        messageFingerprint = fingerprint,
        transactionReference = referenceNumber,
        parsingStatus = status.name,
        importedAt = System.currentTimeMillis(),
        parsingError = parsingError,
        // Raw text is kept only until the user decides; markProcessed() clears it afterwards.
        rawMessage = originalMessage
    )
