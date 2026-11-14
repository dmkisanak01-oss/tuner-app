package com.example.tuner.ui

import androidx.annotation.StringRes
import com.example.tuner.R
import com.example.tuner.data.db.GraphicsMode
import com.example.tuner.domain.downscaleArg
import com.example.tuner.system.BackendState
import com.example.tuner.system.GameMode

@StringRes
fun GameMode.labelRes(): Int = when (this) {
    GameMode.STANDARD -> R.string.mode_standard
    GameMode.PERFORMANCE -> R.string.mode_performance
    GameMode.BATTERY -> R.string.mode_battery
    GameMode.CUSTOM -> R.string.mode_custom
}

/** Label pendek untuk daftar dan chip status. */
@StringRes
fun GameMode.shortLabelRes(): Int = if (this == GameMode.CUSTOM) R.string.mode_custom_short else labelRes()

@StringRes
fun GraphicsMode.labelRes(): Int = when (this) {
    GraphicsMode.HIGH -> R.string.gfx_high
    GraphicsMode.MEDIUM -> R.string.gfx_medium
    GraphicsMode.LOW -> R.string.gfx_low
    GraphicsMode.ULTRA_LOW -> R.string.gfx_ultra
}

/** Persentase resolusi untuk ditampilkan, mis. "85%". */
fun GraphicsMode.percentLabel(): String =
    downscaleArg().toFloatOrNull()?.let { "${(it * 100).toInt()}%" } ?: "100%"

@StringRes
fun BackendState.labelRes(): Int = when (this) {
    BackendState.UNAVAILABLE -> R.string.state_unavailable
    BackendState.NOT_RUNNING -> R.string.state_not_running
    BackendState.NEEDS_PERMISSION -> R.string.state_needs_permission
    BackendState.CONNECTING -> R.string.state_connecting
    BackendState.READY -> R.string.state_ready
}
