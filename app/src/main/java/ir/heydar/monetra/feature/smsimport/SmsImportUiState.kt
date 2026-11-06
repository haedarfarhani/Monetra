package ir.heydar.monetra.feature.smsimport

import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.domain.model.Account
import ir.heydar.monetra.domain.model.BankTransactionDraft
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType

enum class SmsImportStatus {
    IDLE,
    NEEDS_REVIEW,
    NOT_FINANCIAL,
    DUPLICATE,
    SAVED,
    FAILED
}

data class SmsImportUiState(
    val input: String = "",
    val isAnalysing: Boolean = false,
    val isSaving: Boolean = false,
    val draft: BankTransactionDraft? = null,
    val status: SmsImportStatus = SmsImportStatus.IDLE,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val categoryId: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val description: String = "",
    val currency: AppCurrency = AppCurrency.IRR,
    val calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    val language: String = "en",
    val autoSaveEnabled: Boolean = false
) {
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    val categoriesForType: List<Category> get() = categories.filter { it.type == type }
}

sealed interface SmsImportUiEvent {
    data class InputChanged(val value: String) : SmsImportUiEvent
    data object Analyse : SmsImportUiEvent
    data class ChangeType(val type: TransactionType) : SmsImportUiEvent
    data class ChangeAmount(val value: String) : SmsImportUiEvent
    data class SelectAccount(val accountId: Long) : SmsImportUiEvent
    data class SelectCounterAccount(val accountId: Long) : SmsImportUiEvent
    data class SelectCategory(val categoryId: Long?) : SmsImportUiEvent
    data class ChangeDescription(val value: String) : SmsImportUiEvent
    data object Save : SmsImportUiEvent
    data object Reject : SmsImportUiEvent
}
