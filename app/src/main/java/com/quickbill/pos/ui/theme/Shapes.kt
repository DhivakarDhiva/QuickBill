package com.quickbill.pos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =========================================================================
// QuickBill Modern Rounded Shape System
// Medium rounded corners for modern POS products (10.dp - 20.dp, full pill)
// =========================================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val CardShape = RoundedCornerShape(16.dp)
val ProductCardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(24.dp) // Premium Pill shape
val SmallButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(20.dp)
val InputShape = RoundedCornerShape(14.dp)
val DialogShape = RoundedCornerShape(24.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ReceiptShape = RoundedCornerShape(16.dp)
val BadgeShape = RoundedCornerShape(8.dp)
