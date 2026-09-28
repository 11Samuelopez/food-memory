package com.example.foodmemory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared dimensions used by feature screens; values are centralized for consistent refinement. */
object FoodSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Compact = 13.dp
    val CardInset = 18.dp
    val Medium = 16.dp
    val ScreenHorizontal = 22.dp
    val Large = 24.dp
    val ScreenBottom = 28.dp
    val ExtraLarge = 32.dp
}

object FoodCorners {
    val Small = RoundedCornerShape(12.dp)
    val Compact = RoundedCornerShape(16.dp)
    val Medium = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(20.dp)
    val Profile = RoundedCornerShape(22.dp)
    val Large = RoundedCornerShape(24.dp)
    val Feature = RoundedCornerShape(26.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
}

object FoodSizes {
    val Icon = 24.dp
    val TouchTarget = 48.dp
    val ProfileAvatar = 58.dp
}
