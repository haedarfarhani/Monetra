package ir.heydar.monetra.core.finance

import ir.heydar.monetra.domain.model.Transaction
import ir.heydar.monetra.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceEffectTest {

    private fun transaction(
        type: TransactionType,
        accountId: Long = 1L,
        counterAccountId: Long? = null,
        amount: Long = 10_000L
    ) = Transaction(
        accountId = accountId,
        counterAccountId = counterAccountId,
        amountMinorUnits = amount,
        type = type
    )

    @Test
    fun `income increases the account balance`() {
        assertEquals(10_000L, BalanceEffect.deltaFor(transaction(TransactionType.INCOME), 1L))
        assertEquals(0L, BalanceEffect.deltaFor(transaction(TransactionType.INCOME), 2L))
    }

    @Test
    fun `expense decreases the account balance`() {
        assertEquals(-10_000L, BalanceEffect.deltaFor(transaction(TransactionType.EXPENSE), 1L))
    }

    @Test
    fun `transfer moves money between the two accounts`() {
        val transfer = transaction(
            type = TransactionType.TRANSFER,
            accountId = 1L,
            counterAccountId = 2L
        )
        assertEquals(-10_000L, BalanceEffect.deltaFor(transfer, 1L))
        assertEquals(10_000L, BalanceEffect.deltaFor(transfer, 2L))
        assertEquals(0L, BalanceEffect.deltaFor(transfer, 3L))
        assertEquals(listOf(1L, 2L), BalanceEffect.accountsAffectedBy(transfer))
    }

    @Test
    fun `transfer net effect on the user is zero`() {
        val transfer = transaction(
            type = TransactionType.TRANSFER,
            accountId = 1L,
            counterAccountId = 2L
        )
        val net = BalanceEffect.deltaFor(transfer, 1L) + BalanceEffect.deltaFor(transfer, 2L)
        assertEquals(0L, net)
    }
}
