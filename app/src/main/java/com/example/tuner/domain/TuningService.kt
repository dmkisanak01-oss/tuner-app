package com.example.tuner.domain

import com.example.tuner.data.db.ApplyLogEntity
import com.example.tuner.data.db.GameProfileEntity
import com.example.tuner.data.db.TunerDatabase
import com.example.tuner.system.BackendState
import com.example.tuner.system.ExecResult
import com.example.tuner.system.GameCommands
import com.example.tuner.system.GameMode
import com.example.tuner.system.PackageName
import com.example.tuner.system.PrivilegedExecutor
import com.example.tuner.system.SystemProps
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ApplyOutcome {
    data object Ok : ApplyOutcome
    data class Partial(val detail: String) : ApplyOutcome
    data class Failed(val detail: String) : ApplyOutcome
    data object NoPrivilege : ApplyOutcome
    data object InvalidPackage : ApplyOutcome
}

/** Satu penulis pada satu waktu (Mutex). Setiap perintah dicatat ke apply_log. */
class TuningService(
    private val executor: PrivilegedExecutor,
    private val db: TunerDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()

    suspend fun applyGameMode(pkg: String, mode: GameMode): ApplyOutcome {
        if (!PackageName.isValid(pkg) || !db.gameEntryDao().exists(pkg)) return ApplyOutcome.InvalidPackage
        if (executor.state.value != BackendState.READY) return ApplyOutcome.NoPrivilege
        return mutex.withLock {
            val set = run(pkg, "SET_GAME_MODE", GameCommands.setMode(pkg, mode))
            if (!set.ok) {
                saveProfile(pkg, status = "FAILED") { it }
                return@withLock ApplyOutcome.Failed(set.output.ifBlank { "exit ${set.exitCode}" })
            }
            // Baca balik. Format keluaran list-configs belum diverifikasi, jadi hanya exit code yang dicek.
            val read = run(pkg, "READ_BACK", GameCommands.listConfigs(pkg))
            val status = if (read.ok) "OK" else "PARTIAL"
            saveProfile(pkg, status = status) { it.copy(tuneEnabled = true, gameMode = mode) }
            if (read.ok) ApplyOutcome.Ok else ApplyOutcome.Partial(read.output.ifBlank { "exit ${read.exitCode}" })
        }
    }

    suspend fun resetGame(pkg: String): ApplyOutcome {
        if (!PackageName.isValid(pkg) || !db.gameEntryDao().exists(pkg)) return ApplyOutcome.InvalidPackage
        if (executor.state.value != BackendState.READY) return ApplyOutcome.NoPrivilege
        return mutex.withLock {
            val r = run(pkg, "RESET", GameCommands.reset(pkg))
            if (!r.ok) {
                saveProfile(pkg, status = "FAILED") { it }
                return@withLock ApplyOutcome.Failed(r.output.ifBlank { "exit ${r.exitCode}" })
            }
            saveProfile(pkg, status = "OK") { it.copy(tuneEnabled = false, gameMode = GameMode.STANDARD) }
            ApplyOutcome.Ok
        }
    }

    /** Menjalankan perintah baca-saja dan mengembalikan teks untuk ditampilkan. Tidak mengubah setelan apa pun. */
    suspend fun diagnose(pkg: String): String {
        if (!PackageName.isValid(pkg) || !db.gameEntryDao().exists(pkg)) return "Game tidak dikenali"
        if (executor.state.value != BackendState.READY) return "Butuh akses sistem"
        val commands = listOf(
            SystemProps.get("ro.product.manufacturer"),
            SystemProps.get("ro.product.model"),
            SystemProps.get("ro.build.version.release"),
            SystemProps.get("ro.build.version.sdk"),
            GameCommands.help(),
            GameCommands.listModes(pkg),
            GameCommands.listConfigs(pkg),
        )
        return mutex.withLock {
            val blocks = mutableListOf<String>()
            for (argv in commands) {
                val r = executor.exec(argv)
                blocks += "$ ${argv.joinToString(" ")}\n[exit ${r.exitCode}]\n${r.output.trim().take(1_500)}"
            }
            blocks.joinToString("\n\n")
        }
    }

    private suspend fun run(pkg: String, action: String, argv: List<String>): ExecResult {
        val result = executor.exec(argv)
        db.applyLogDao().insert(
            ApplyLogEntity(
                scope = "GAME",
                packageName = pkg,
                action = action,
                command = argv.joinToString(" "),
                backend = result.backend?.name ?: "NONE",
                exitCode = result.exitCode,
                output = result.output.take(MAX_LOG_CHARS),
                createdAt = clock(),
            ),
        )
        db.applyLogDao().prune(MAX_LOG_ROWS)
        return result
    }

    private suspend fun saveProfile(pkg: String, status: String, change: (GameProfileEntity) -> GameProfileEntity) {
        val now = clock()
        val dao = db.gameProfileDao()
        val base = dao.get(pkg) ?: GameProfileEntity(packageName = pkg, createdAt = now, updatedAt = now)
        dao.upsert(change(base).copy(lastAppliedAt = now, lastApplyStatus = status, updatedAt = now))
    }

    private companion object {
        const val MAX_LOG_CHARS = 4_000
        const val MAX_LOG_ROWS = 500
    }
}
