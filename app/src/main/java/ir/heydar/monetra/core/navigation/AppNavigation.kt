package ir.heydar.monetra.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.glass.LiquidGlassBackdrop
import ir.heydar.monetra.core.designsystem.glass.glassStrokeBrush
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.feature.accounts.AccountsRoute
import ir.heydar.monetra.feature.analytics.AnalyticsRoute
import ir.heydar.monetra.feature.bankreview.BankTransactionReviewRoute
import ir.heydar.monetra.feature.categories.CategoriesRoute
import ir.heydar.monetra.feature.dashboard.DashboardRoute
import ir.heydar.monetra.feature.onboarding.OnboardingRoute
import ir.heydar.monetra.feature.settings.SettingsRoute
import ir.heydar.monetra.feature.smsimport.SmsImportRoute
import ir.heydar.monetra.feature.transactiondetail.TransactionDetailRoute
import ir.heydar.monetra.feature.transactionform.TransactionFormRoute
import ir.heydar.monetra.feature.transactions.TransactionsRoute

@Composable
fun AppNavigation(
    isFirstLaunch: Boolean,
    sharedText: String? = null,
    pendingTransactionId: Long? = null,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // A message shared from the SMS app opens the import screen; the view model consumes the text.
    LaunchedEffect(sharedText, isFirstLaunch) {
        if (!isFirstLaunch && !sharedText.isNullOrBlank()) {
            navController.navigate(Screen.SmsImport)
        }
    }

    LaunchedEffect(pendingTransactionId, isFirstLaunch) {
        if (!isFirstLaunch && pendingTransactionId != null && pendingTransactionId > 0L) {
            navController.navigate(Screen.BankTransactionReview(pendingTransactionId))
        }
    }

    val isTopLevelDestination = TopLevelDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (isTopLevelDestination) {
                ModernFloatingNavigationBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = {
                        navController.navigate(Screen.AddTransaction())
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Aurora gradient backdrop — every glass panel refracts it.
            LiquidGlassBackdrop(modifier = Modifier.fillMaxSize())

            // Content spans the full height so it scrolls UNDER the glass bar.
            // Screens keep their own bottom content padding; only the
            // status-bar inset is applied here.
            NavHost(
                navController = navController,
                startDestination = if (isFirstLaunch) Screen.Onboarding else Screen.Dashboard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
            composable<Screen.Onboarding> {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo<Screen.Onboarding> { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Dashboard> {
                DashboardRoute(
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction(transactionType = type))
                    },
                    onNavigateToTransactionDetail = { id ->
                        navController.navigate(Screen.TransactionDetail(id))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings)
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Analytics)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts)
                    }
                )
            }
            composable<Screen.Transactions> {
                TransactionsRoute(
                    onNavigateToDetail = { id -> navController.navigate(Screen.TransactionDetail(id)) },
                    onNavigateToAdd = { navController.navigate(Screen.AddTransaction()) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.AddTransaction(id)) }
                )
            }
            composable<Screen.Analytics> {
                AnalyticsRoute(onBack = { navController.popBackStack() })
            }
            composable<Screen.Accounts> {
                AccountsRoute()
            }
            composable<Screen.Categories> {
                CategoriesRoute()
            }
            composable<Screen.Settings> {
                SettingsRoute(
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories)
                    }
                )
            }
            composable<Screen.AddTransaction> {
                TransactionFormRoute(
                    onFinished = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable<Screen.SmsImport> {
                SmsImportRoute(onDone = { navController.popBackStack() })
            }
            composable<Screen.TransactionDetail> { entry ->
                val route = entry.toRoute<Screen.TransactionDetail>()
                TransactionDetailRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.AddTransaction(id)) }
                )
            }
            composable<Screen.BankTransactionReview> {
                BankTransactionReviewRoute(
                    onDone = { navController.popBackStack() }
                )
            }
        }
        }
    }
}

/**
 * Premium Modern Floating Navigation Bar with a center Floating Action Button.
 * Features:
 * - Floating frosted glass capsule (Liquid Glass fill + bright hairline rim)
 * - Center gradient Floating Action Button for quick transaction creation
 * - Smooth spring-animated indicator pill for selected destination
 * - Icon transition from Outlined to Filled with bounce scale effect
 * - Responsive typography & touch targets
 */
@Composable
private fun ModernFloatingNavigationBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    onFabClick: () -> Unit
) {
    val barShape = RoundedCornerShape(28.dp)
    val primaryColor = MaterialTheme.colorScheme.primary
    val extended = MaterialTheme.monetraColors
    val glass = MaterialTheme.glassColors

    // 4 destinations symmetrically divided around the center FAB
    val leftDestinations = listOf(TopLevelDestination.DASHBOARD, TopLevelDestination.TRANSACTIONS)
    val rightDestinations = listOf(TopLevelDestination.ACCOUNTS, TopLevelDestination.SETTINGS)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Glass Bar — dense frosted capsule over the Aurora backdrop.
        // (Backdrop blur via Haze was removed: its RenderEffect output showed a
        // visible horizontal band on some devices; the smooth Aurora behind the
        // translucent fill delivers the same frosted look without artifacts.)
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = barShape,
            fill = Brush.verticalGradient(
                listOf(glass.barTint, glass.barTint.copy(alpha = glass.barTint.alpha * 0.82f))
            ),
            showSheen = true,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 Items
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftDestinations.forEach { dest ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(dest.route::class)
                        } == true

                        FloatingNavItem(
                            destination = dest,
                            isSelected = isSelected,
                            primaryColor = primaryColor,
                            unselectedColor = extended.navItemInactive,
                            pillColor = extended.navItemPill,
                            onClick = { onNavigate(dest) }
                        )
                    }
                }

                // Center Spacer for FAB clearance
                Spacer(modifier = Modifier.width(64.dp))

                // Right 2 Items
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightDestinations.forEach { dest ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(dest.route::class)
                        } == true

                        FloatingNavItem(
                            destination = dest,
                            isSelected = isSelected,
                            primaryColor = primaryColor,
                            unselectedColor = extended.navItemInactive,
                            pillColor = extended.navItemPill,
                            onClick = { onNavigate(dest) }
                        )
                    }
                }
            }
        }

        // Center Floating Action Button — brand gradient with a glass rim,
        // centered exactly in the middle of the bar.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(primaryColor, MaterialTheme.colorScheme.secondary)
                    )
                )
                .border(BorderStroke(Dimens.glassStrokeWidth, glassStrokeBrush()), CircleShape)
                .clickable(onClick = onFabClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = R.string.add_transaction),
                // onPrimary flips with the theme: white on Turf Green (light),
                // dark coffee on Willow Green (dark) — always readable.
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    destination: TopLevelDestination,
    isSelected: Boolean,
    primaryColor: Color,
    unselectedColor: Color,
    pillColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated scale on selection with spring
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navItemScale"
    )

    // Animated colors
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else unselectedColor,
        animationSpec = tween(durationMillis = 200),
        label = "navIconColor"
    )

    // Solid themed pill behind the selected icon — clearly visible in both themes.
    val animatedPillColor by animateColorAsState(
        targetValue = if (isSelected) pillColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "navPillColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon capsule background
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(animatedPillColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = stringResource(id = destination.titleRes),
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(id = destination.titleRes),
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
