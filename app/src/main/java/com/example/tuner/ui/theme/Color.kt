package com.example.tuner.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Diturunkan dari replica/design/tokens.json dan tokens.dark.json. Kalau token berubah, ubah JSON, jalankan contrast.py, lalu samakan nilai di sini.
// Nilai = placeholder netral. replica-brand mengganti nilainya, nama role tetap.

@Immutable
data class AppColors(
    val bg: Color, val surface: Color, val border: Color, val borderInput: Color,
    val text: Color, val textMuted: Color, val accent: Color, val onAccent: Color,
    val danger: Color, val onDanger: Color, val success: Color, val warning: Color,
)

val LightAppColors = AppColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFF3F5F8),
    border = Color(0xFFE1E5EA),
    borderInput = Color(0xFF7A838F),
    text = Color(0xFF14171C),
    textMuted = Color(0xFF555E6B),
    accent = Color(0xFF2F5BEA),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFB3261E),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF1B6E46),
    warning = Color(0xFF8A5300),
)

val DarkAppColors = AppColors(
    bg = Color(0xFF0F1217),
    surface = Color(0xFF181C23),
    border = Color(0xFF2A303A),
    borderInput = Color(0xFF7D8794),
    text = Color(0xFFECEFF3),
    textMuted = Color(0xFFA3ACB8),
    accent = Color(0xFF9DB4FF),
    onAccent = Color(0xFF0B1330),
    danger = Color(0xFFFF8A80),
    onDanger = Color(0xFF3B0A06),
    success = Color(0xFF5CCF93),
    warning = Color(0xFFF0B44C),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
