package ir.heydar.monetra.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MonetraExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val divider: Color,
    val surfaceContainer: Color,
    val balanceGradientStart: Color,
    val balanceGradientMid: Color,
    val balanceGradientEnd: Color
)

val LightExtendedColors = MonetraExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    info = InfoLight,
    onInfo = OnInfoLight,
    infoContainer = InfoContainerLight,
    income = IncomeColorLight,
    expense = ExpenseColorLight,
    transfer = TransferColorLight,
    divider = DividerLight,
    surfaceContainer = SurfaceContainerLight,
    balanceGradientStart = BalanceGradientStartLight,
    balanceGradientMid = BalanceGradientMidLight,
    balanceGradientEnd = BalanceGradientEndLight
)

val DarkExtendedColors = MonetraExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    info = InfoDark,
    onInfo = OnInfoDark,
    infoContainer = InfoContainerDark,
    income = IncomeColorDark,
    expense = ExpenseColorDark,
    transfer = TransferColorDark,
    divider = DividerDark,
    surfaceContainer = SurfaceContainerDark,
    balanceGradientStart = BalanceGradientStartDark,
    balanceGradientMid = BalanceGradientMidDark,
    balanceGradientEnd = BalanceGradientEndDark
)

val LocalMonetraColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.monetraColors: MonetraExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMonetraColors.current
