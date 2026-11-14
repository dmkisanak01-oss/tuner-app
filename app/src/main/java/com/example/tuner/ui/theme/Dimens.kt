package com.example.tuner.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Diturunkan dari tokens.json.
object AppSpace {
    val s0 = 0.dp
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp
    val s64 = 64.dp
}

object AppShape {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
}

object AppSize {
    val touchMin = 48.dp
    val icon = 24.dp
    val listItem = 72.dp
    val topBar = 64.dp
    val contentMaxWidth = 640.dp
}

object AppMotion {
    const val FAST = 120
    const val BASE = 200
    const val SLOW = 320
    val Easing = CubicBezierEasing(.2f, .8f, .2f, 1f)
}
