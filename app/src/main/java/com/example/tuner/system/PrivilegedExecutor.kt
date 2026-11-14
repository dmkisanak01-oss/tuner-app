package com.example.tuner.system

import kotlinx.coroutines.flow.StateFlow

enum class Backend { SHIZUKU, ROOT }

enum class BackendState {
    UNAVAILABLE,      // Shizuku tidak terpasang / root tidak ada
    NOT_RUNNING,      // Shizuku terpasang tapi belum dijalankan
    NEEDS_PERMISSION, // belum diberi izin
    CONNECTING,       // izin ada, layanan sedang tersambung
    READY,
}

data class ExecResult(val exitCode: Int, val output: String, val backend: Backend?) {
    val ok: Boolean get() = exitCode == 0
}

interface PrivilegedExecutor {
    val state: StateFlow<BackendState>

    /** Cek ulang kondisi backend. Boleh memunculkan prompt su, jadi jangan panggil dari main thread. */
    suspend fun refresh()

    /** Jalankan satu perintah. Setiap argumen harus lolos [SafeArgs]. */
    suspend fun exec(argv: List<String>): ExecResult
}

/** Pertahanan lapis kedua setelah validasi di [GameCommands]: hanya karakter aman, tanpa spasi atau metakarakter shell. */
object SafeArgs {
    private val SAFE = Regex("^[A-Za-z0-9._=:/+-]+$")
    fun check(argv: List<String>) {
        require(argv.isNotEmpty()) { "Perintah kosong" }
        require(argv.all { SAFE.matches(it) }) { "Argumen mengandung karakter tidak aman" }
    }
}
