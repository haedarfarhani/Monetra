package ir.heydar.monetra.feature.dashboard

import app.cash.turbine.test
import ir.heydar.monetra.TestDispatcherRule
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.AccountRepository
import ir.heydar.monetra.domain.repository.CategoryRepository
import ir.heydar.monetra.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val accountRepository: AccountRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val categoryRepository: CategoryRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private val sampleTransactions = listOf(
        Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            amountMinorUnits = 100_000L,
            type = TransactionType.INCOME,
            description = "Salary"
        )
    )

    private val sampleAccounts = listOf(
        ir.heydar.monetra.domain.model.Account(
            id = 1L,
            name = "Main",
            type = "BANK",
            balance = 500_000L,
            isDefault = true
        ),
        ir.heydar.monetra.domain.model.Account(
            id = 2L,
            name = "Savings",
            type = "BANK",
            balance = 250_000L,
            isDefault = false
        )
    )

    @Before
    fun setup() {
        every { accountRepository.getAllAccounts() } returns flowOf(sampleAccounts)
        every { transactionRepository.getTotalIncome(any(), any()) } returns flowOf(200_000L)
        every { transactionRepository.getTotalExpense(any(), any()) } returns flowOf(50_000L)
        every { transactionRepository.getRecentTransactions(any()) } returns flowOf(sampleTransactions)
        every { transactionRepository.getAllTransactions() } returns flowOf(sampleTransactions)
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        every { preferencesManager.currency } returns flowOf("USD")
        every { preferencesManager.calendarSystem } returns flowOf("gregorian")
        every { preferencesManager.language } returns flowOf("en")
    }

    private fun viewModel() = DashboardViewModel(
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
        categoryRepository = categoryRepository,
        preferencesManager = preferencesManager,
        dispatcherProvider = dispatcherRule.testDispatcherProvider
    )

    @Test
    fun loadDashboardData_updatesUiStateWithLoadedDataSuccessfully() = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(500_000L, state.totalBalance)
        assertEquals(200_000L, state.monthlyIncome)
        assertEquals(50_000L, state.monthlyExpense)
        assertEquals(sampleTransactions, state.recentTransactions)
        assertEquals(4, state.monthlyTrend.size)
    }

    @Test
    fun addTransactionClicked_eventTriggersNavigateToAddTransactionEffect() = runTest {
        val viewModel = viewModel()
        viewModel.uiEffect.test {
            viewModel.onEvent(DashboardUiEvent.AddTransactionClicked)
            assertEquals(DashboardUiEffect.NavigateToAddTransaction, awaitItem())
        }
    }

    @Test
    fun transactionClicked_eventTriggersNavigateToTransactionDetailEffect() = runTest {
        val viewModel = viewModel()
        viewModel.uiEffect.test {
            viewModel.onEvent(DashboardUiEvent.TransactionClicked(1L))
            assertEquals(DashboardUiEffect.NavigateToTransactionDetail(1L), awaitItem())
        }
    }

    @Test
    fun togglePrivacy_eventFlipsPrivacyFlag() = runTest {
        val viewModel = viewModel()
        val initial = viewModel.uiState.value.isPrivacyEnabled

        viewModel.onEvent(DashboardUiEvent.TogglePrivacy)
        assertEquals(!initial, viewModel.uiState.value.isPrivacyEnabled)

        viewModel.onEvent(DashboardUiEvent.TogglePrivacy)
        assertEquals(initial, viewModel.uiState.value.isPrivacyEnabled)
    }
}
