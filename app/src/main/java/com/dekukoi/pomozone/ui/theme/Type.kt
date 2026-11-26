package com.dekukoi.pomozone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Time in the ring
    displayLarge = TextStyle(
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    ),
    // Card values and goal label
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    // Tab labels
    labelLarge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    // Mode chips and panel titles
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.em),
    // Card titles
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.em),
)
