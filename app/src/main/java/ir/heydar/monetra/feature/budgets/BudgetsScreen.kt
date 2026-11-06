package ir.heydar.monetra.feature.budgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Budgets")
        },
        modifier = modifier
    ) { innerPadding ->
        EmptyState(
            message = "No budgets configured.",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

