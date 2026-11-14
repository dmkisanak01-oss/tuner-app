package com.example.tuner.system

import com.example.tuner.data.prefs.PrivilegeMode
import com.example.tuner.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Meneruskan ke backend yang dipilih user di S02. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExecutorRouter(
    private val settings: SettingsStore,
    private val shizuku: ShizukuExecutor,
    private val root: RootExecutor,
    scope: CoroutineScope,
) : PrivilegedExecutor {

    override val state: StateFlow<BackendState> = settings.privilegeMode
        .flatMapLatest { mode ->
            when (mode) {
                PrivilegeMode.SHIZUKU -> shizuku.state
                PrivilegeMode.ROOT -> root.state
                PrivilegeMode.NONE -> flowOf(BackendState.UNAVAILABLE)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, BackendState.UNAVAILABLE)

    override suspend fun refresh() {
        when (settings.privilegeMode.first()) {
            PrivilegeMode.SHIZUKU -> shizuku.refresh()
            PrivilegeMode.ROOT -> root.refresh()
            PrivilegeMode.NONE -> Unit
        }
    }

    override suspend fun exec(argv: List<String>): ExecResult = when (settings.privilegeMode.first()) {
        PrivilegeMode.SHIZUKU -> shizuku.exec(argv)
        PrivilegeMode.ROOT -> root.exec(argv)
        PrivilegeMode.NONE -> ExecResult(-1, "Belum ada akses", null)
    }
}
