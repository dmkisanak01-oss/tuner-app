package com.example.tuner.ui

import androidx.annotation.StringRes
import com.example.tuner.R
import com.example.tuner.system.BackendState
import com.example.tuner.system.GameMode

@StringRes
fun GameMode.labelRes(): Int = when (this) {
    GameMode.STANDARD -> R.string.mode_standard
    GameMode.PERFORMANCE -> R.string.mode_performance
    GameMode.BATTERY -> R.string.mode_battery
    GameMode.CUSTOM -> R.string.mode_custom
}

@StringRes
fun BackendState.labelRes(): Int = when (this) {
    BackendState.UNAVAILABLE -> R.string.state_unavailable
    BackendState.NOT_RUNNING -> R.string.state_not_running
    BackendState.NEEDS_PERMISSION -> R.string.state_needs_permission
    BackendState.CONNECTING -> R.string.state_connecting
    BackendState.READY -> R.string.state_ready
}
