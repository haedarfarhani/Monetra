package ir.heydar.monetra.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.domain.model.TransactionType

fun categoryIcon(name: String?): ImageVector {
    if (name == null) return Icons.Outlined.Category
    val lower = name.lowercase()
    return when {
        lower.contains("food") || lower.contains("غذا") || lower.contains("خوراک") || lower.contains("restaurant") -> Icons.Outlined.Restaurant
        lower.contains("cafe") || lower.contains("coffee") || lower.contains("کافه") -> Icons.Outlined.LocalCafe
        lower.contains("grocery") || lower.contains("market") || lower.contains("سوپر") || lower.contains("خرید") -> Icons.Outlined.LocalGroceryStore
        lower.contains("shop") || lower.contains("clothing") || lower.contains("لباس") || lower.contains("پوشاک") || lower.contains("مد") -> Icons.Outlined.ShoppingBag
        lower.contains("transport") || lower.contains("car") || lower.contains("ماشین") || lower.contains("خودرو") || lower.contains("حمل") || lower.contains("بنزین") -> Icons.Outlined.DirectionsCar
        lower.contains("travel") || lower.contains("flight") || lower.contains("سفر") || lower.contains("هتل") -> Icons.Outlined.Flight
        lower.contains("health") || lower.contains("medical") || lower.contains("hospital") || lower.contains("سلامت") || lower.contains("درمان") || lower.contains("دارو") || lower.contains("پزشک") -> Icons.Outlined.LocalHospital
        lower.contains("education") || lower.contains("school") || lower.contains("آموزش") || lower.contains("تحصیل") || lower.contains("کتاب") -> Icons.Outlined.School
        lower.contains("entertainment") || lower.contains("movie") || lower.contains("سینما") || lower.contains("سرگرمی") || lower.contains("بازی") -> Icons.Outlined.Movie
        lower.contains("sport") || lower.contains("gym") || lower.contains("ورزش") || lower.contains("باشگاه") -> Icons.Outlined.FitnessCenter
        lower.contains("phone") || lower.contains("mobile") || lower.contains("موبایل") || lower.contains("اینترنت") || lower.contains("شارژ") -> Icons.Outlined.PhoneAndroid
        lower.contains("home") || lower.contains("rent") || lower.contains("خانه") || lower.contains("مسکن") || lower.contains("اجاره") -> Icons.Outlined.Home
        lower.contains("salary") || lower.contains("income") || lower.contains("حقوق") || lower.contains("دستمزد") || lower.contains("درآمد") -> Icons.Outlined.Work
        lower.contains("bonus") || lower.contains("پاداش") || lower.contains("عیدی") -> Icons.Outlined.AttachMoney
        lower.contains("invest") || lower.contains("سرمایه") || lower.contains("بورس") || lower.contains("سود") -> Icons.Outlined.AttachMoney
        lower.contains("sale") || lower.contains("فروش") || lower.contains("کسب") || lower.contains("تجارت") -> Icons.Outlined.Payments
        lower.contains("gift") || lower.contains("هدیه") || lower.contains("کادو") || lower.contains("عیدی") -> Icons.Outlined.CardGiftcard
        lower.contains("transfer") || lower.contains("انتقال") || lower.contains("کارت") -> Icons.Outlined.SwapHoriz
        lower.contains("bank") || lower.contains("بانک") -> Icons.Outlined.AccountBalance
        lower.contains("bill") || lower.contains("قبض") || lower.contains("آب") || lower.contains("برق") || lower.contains("گاز") -> Icons.Outlined.Payments
        lower.contains("money") || lower.contains("پول") -> Icons.Outlined.AttachMoney
        else -> Icons.Outlined.Category
    }
}

@Composable
fun categoryIconColors(type: TransactionType): Pair<Color, Color> {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bg = when (type) {
        TransactionType.INCOME -> if (isDark) Color(0xFF1E4A38) else Color(0xFFD3EEDF)   // turf family
        TransactionType.EXPENSE -> if (isDark) Color(0xFF64151C) else Color(0xFFFBDCDD)  // scarlet family
        TransactionType.TRANSFER -> if (isDark) Color(0xFF4A3325) else Color(0xFFEFDFD4) // coffee family
    }
    val tint = when (type) {
        TransactionType.INCOME -> if (isDark) Color(0xFF87C38F) else Color(0xFF226F54)
        TransactionType.EXPENSE -> if (isDark) Color(0xFFF2767F) else Color(0xFFDA2C38)
        TransactionType.TRANSFER -> if (isDark) Color(0xFFD8B79C) else Color(0xFF5F3D2C)
    }
    return bg to tint
}

@Composable
fun MonetraCategoryIcon(
    categoryName: String?,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.categoryIconCircle
) {
    val (bg, tint) = categoryIconColors(transactionType)
    val icon = categoryIcon(categoryName)

    Box(
        modifier = modifier
            .size(size)
            .background(color = bg, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
