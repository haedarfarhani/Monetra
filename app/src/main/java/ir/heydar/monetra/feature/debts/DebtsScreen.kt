package ir.heydar.monetra.feature.debts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState

@Composable
fun DebtsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Debts & Loans")
        },
        modifier = modifier
    ) { innerPadding ->
        EmptyState(
            message = "No debts recorded.",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

