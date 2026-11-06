package ir.heydar.monetra.feature.savings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState

@Composable
fun SavingsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Savings Goals")
        },
        modifier = modifier
    ) { innerPadding ->
        EmptyState(
            message = "No savings goals created.",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

