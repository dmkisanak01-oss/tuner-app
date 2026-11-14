package com.example.tuner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Material 3 dipakai sebagai basis komponen. AppColors dipakai untuk role di luar M3 (success, warning, borderInput).
// Dynamic color (Material You) SENGAJA tidak dipakai di v1 agar kontras tetap terukur; boleh jadi opsi di S10.
@Composable
fun TunerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkAppColors else LightAppColors
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.bg, onSurface = c.text, surfaceVariant = c.surface, onSurfaceVariant = c.textMuted,
        outline = c.borderInput, outlineVariant = c.border, error = c.danger, onError = c.onDanger,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.bg, onSurface = c.text, surfaceVariant = c.surface, onSurfaceVariant = c.textMuted,
        outline = c.borderInput, outlineVariant = c.border, error = c.danger, onError = c.onDanger,
    )
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
}
