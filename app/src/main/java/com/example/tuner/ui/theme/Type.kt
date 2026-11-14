package com.example.tuner.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Diturunkan dari tokens.json. Font bundel (Inter, OFL) bisa ditambah lewat res/font; default = font sistem.
object AppType {
    val xs = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight(500))
    val sm = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight(400))
    val base = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight(400))
    val lg = TextStyle(fontFamily = FontFamily.Default, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight(600))
    val xl = TextStyle(fontFamily = FontFamily.Default, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight(700))
    val display = TextStyle(fontFamily = FontFamily.Default, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight(700))
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight(400))
}
