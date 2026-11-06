package ir.heydar.monetra.data.repository

import ir.heydar.monetra.domain.model.SmsParseResult
import ir.heydar.monetra.domain.repository.SmsTransactionRepository
import ir.heydar.monetra.domain.usecase.CreatePendingBankTransactionUseCase
import ir.heydar.monetra.feature.smsimport.parser.BankMessageParserRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTransactionRepositoryImpl @Inject constructor(
    private val parserRegistry: BankMessageParserRegistry,
    private val createPendingUseCase: CreatePendingBankTransactionUseCase
) : SmsTransactionRepository {

    override fun parseSms(rawMessage: String, sender: String?): SmsParseResult {
        return parserRegistry.parseSmsResult(rawMessage, sender)
    }

    override suspend fun processIncomingSms(rawMessage: String, sender: String?): Long {
        val draft = parserRegistry.parse(rawMessage, sender)
        if (draft.parsingStatus == ir.heydar.monetra.domain.model.ParsingStatus.FAILED) {
            return -1L
        }
        return createPendingUseCase(draft)
    }

    override suspend fun isDuplicate(fingerprint: String): Boolean {
        return fingerprint.isBlank()
    }
}
