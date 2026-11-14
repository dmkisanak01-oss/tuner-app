package com.example.tuner.system

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Backend Root lewat libsu. VERIFY: API libsu 6.x, belum dikompilasi atau diuji di perangkat. */
class RootExecutor : PrivilegedExecutor {
    private val _state = MutableStateFlow(BackendState.UNAVAILABLE)
    override val state: StateFlow<BackendState> = _state
    private val mutex = Mutex()

    /** Memunculkan prompt su untuk pertama kali. */
    override suspend fun refresh() {
        _state.value = BackendState.CONNECTING
        val granted = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
        _state.value = if (granted) BackendState.READY else BackendState.UNAVAILABLE
    }

    override suspend fun exec(argv: List<String>): ExecResult {
        SafeArgs.check(argv)
        if (_state.value != BackendState.READY) return ExecResult(-1, "Root tidak siap", Backend.ROOT)
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                // Semua argumen sudah lolos SafeArgs (tanpa spasi atau metakarakter), jadi join dengan spasi aman.
                val result = Shell.cmd(argv.joinToString(" ")).exec()
                ExecResult(result.code, (result.out + result.err).joinToString("\n"), Backend.ROOT)
            }
        }
    }
}
