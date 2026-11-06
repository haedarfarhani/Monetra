package ir.heydar.monetra.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * Brand palette:
 *  Scarlet Rush  #DA2C38 — energetic accent (expense, error, tertiary pop)
 *  Turf Green    #226F54 — brand primary (money, growth, actions)
 *  Willow Green  #87C38F — soft herbal (dark-theme primary, containers)
 *  Lemon Chiffon #F4F0BB — pastel warmth (warning container, light backdrop)
 *  Dark Coffee   #43291F — grounded elegance (secondary, transfer, dark base)
 */

// Light Theme Brand Colors
val PrimaryLight = Color(0xFF226F54)          // Turf Green
val PrimaryVariantLight = Color(0xFF1A5A44)   // deeper turf
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD3EEDF) // willow mint
val OnPrimaryContainerLight = Color(0xFF10382A)

val SecondaryLight = Color(0xFF5F3D2C)        // Dark Coffee (readable on light)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFEFDFD4) // latte
val OnSecondaryContainerLight = Color(0xFF2C1810)

val TertiaryLight = Color(0xFFDA2C38)         // Scarlet Rush
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFBDCDD)
val OnTertiaryContainerLight = Color(0xFF411014)

val ErrorLight = Color(0xFFC22F39)            // deepened Scarlet Rush
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFBDCDD)
val OnErrorContainerLight = Color(0xFF411014)

val BackgroundLight = Color(0xFFF7F6EF)       // chiffon-washed warm cream
val OnBackgroundLight = Color(0xFF241C16)     // dark coffee text
val SurfaceLight = Color(0xFFFFFDF8)
val OnSurfaceLight = Color(0xFF241C16)
val SurfaceVariantLight = Color(0xFFEEEAE0)
val OnSurfaceVariantLight = Color(0xFF6B6157) // warm gray
val SurfaceContainerLight = Color(0xFFF4F1E8)

val OutlineLight = Color(0xFFDFD9CC)
val DividerLight = Color(0xFFE8E3D7)

// Dark Theme Brand Colors
val PrimaryDark = Color(0xFF87C38F)           // Willow Green
val PrimaryVariantDark = Color(0xFF5FA873)
val OnPrimaryDark = Color(0xFF12281C)
val PrimaryContainerDark = Color(0xFF1E4A38)  // deep turf
val OnPrimaryContainerDark = Color(0xFFD3EEDF)

val SecondaryDark = Color(0xFFD8B79C)         // latte
val OnSecondaryDark = Color(0xFF2C1810)
val SecondaryContainerDark = Color(0xFF4A3325) // coffee
val OnSecondaryContainerDark = Color(0xFFEFDFD4)

val TertiaryDark = Color(0xFFF2767F)          // bright scarlet
val OnTertiaryDark = Color(0xFF411014)
val TertiaryContainerDark = Color(0xFF64151C)
val OnTertiaryContainerDark = Color(0xFFFBDCDD)

val ErrorDark = Color(0xFFF26D76)
val OnErrorDark = Color(0xFF411014)
val ErrorContainerDark = Color(0xFF64151C)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF0F0C0A)        // warm near-black coffee
val OnBackgroundDark = Color(0xFFEFE9E1)
val SurfaceDark = Color(0xFF1A1512)
val OnSurfaceDark = Color(0xFFEFE9E1)
val SurfaceVariantDark = Color(0xFF262019)
val OnSurfaceVariantDark = Color(0xFFB8AE9F)
val SurfaceContainerDark = Color(0xFF201A15)

val OutlineDark = Color(0xFF3E362C)
val DividerDark = Color(0xFF332C23)

// Financial Semantic Colors - Light
val SuccessLight = Color(0xFF226F54)          // Turf Green
val OnSuccessLight = Color(0xFFFFFFFF)
val SuccessContainerLight = Color(0xFFD3EEDF)

val WarningLight = Color(0xFF8A7B1F)          // deep chiffon gold
val OnWarningLight = Color(0xFFFFFFFF)
val WarningContainerLight = Color(0xFFF4F0BB) // Lemon Chiffon

val InfoLight = Color(0xFF5F3D2C)             // Dark Coffee
val OnInfoLight = Color(0xFFFFFFFF)
val InfoContainerLight = Color(0xFFEFDFD4)

val IncomeColorLight = Color(0xFF226F54)      // Turf Green
val ExpenseColorLight = Color(0xFFDA2C38)     // Scarlet Rush
val TransferColorLight = Color(0xFF5F3D2C)    // Dark Coffee

// Financial Semantic Colors - Dark
val SuccessDark = Color(0xFF87C38F)           // Willow Green
val OnSuccessDark = Color(0xFF0F2A1E)
val SuccessContainerDark = Color(0xFF1E4A38)

val WarningDark = Color(0xFFE3D98A)           // bright chiffon
val OnWarningDark = Color(0xFF2C2607)
val WarningContainerDark = Color(0xFF4A4415)

val InfoDark = Color(0xFFD8B79C)              // latte
val OnInfoDark = Color(0xFF2C1810)
val InfoContainerDark = Color(0xFF4A3325)

val IncomeColorDark = Color(0xFF82D3A1)       // bright mint
val ExpenseColorDark = Color(0xFFF2767F)      // bright scarlet
val TransferColorDark = Color(0xFFD8B79C)     // latte

// Gradient stops for Balance Card — Turf Green into Dark Coffee
val BalanceGradientStartLight = Color(0xFF226F54)
val BalanceGradientMidLight = Color(0xFF35624A)
val BalanceGradientEndLight = Color(0xFF43291F)
val BalanceGradientStartDark = Color(0xFF2E8A67)
val BalanceGradientMidDark = Color(0xFF4A5B45)
val BalanceGradientEndDark = Color(0xFF6B4A38)

// Aurora backdrop for the Liquid Glass design (drawn behind all screens)
val AuroraBaseLight = Color(0xFFFAF8EF)       // chiffon-washed cream
val AuroraBlobsLight = listOf(
    Color(0xFFC9E7CE), // willow pastel
    Color(0xFFBFE0D2), // turf pastel
    Color(0xFFF8D7D8), // scarlet pastel
    Color(0xFFF4F0BB), // lemon chiffon
    Color(0xFFEAD9CB)  // latte
)

val AuroraBaseDark = Color(0xFF120C09)        // deep coffee black
val AuroraBlobsDark = listOf(
    Color(0xFF1E5741), // deep turf
    Color(0xFF5C1A20), // deep scarlet
    Color(0xFF43291F), // dark coffee
    Color(0xFF2E5B3C), // deep willow
    Color(0xFF454021)  // deep chiffon olive
)

// Compatibility aliases
val IncomeGreen = IncomeColorLight
val ExpenseRed = ExpenseColorLight
val TransferBlue = TransferColorLight
