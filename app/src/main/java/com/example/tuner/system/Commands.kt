package com.example.tuner.system

/** Mode Game Mode API. Nilai cmd diambil dari enum, tidak pernah dari input mentah. */
enum class GameMode(val cmdValue: String) {
    STANDARD("standard"),
    PERFORMANCE("performance"),
    BATTERY("battery"),
}

object PackageName {
    private val REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    fun isValid(value: String): Boolean = value.length <= 255 && REGEX.matches(value)
}

/** getprop hanya untuk kunci yang ada di daftar putih (baca-saja). */
object SystemProps {
    private val ALLOWED = setOf(
        "ro.build.version.sdk", "ro.build.version.release", "ro.product.manufacturer", "ro.product.model",
    )

    fun get(key: String): List<String> {
        require(key in ALLOWED) { "Properti tidak diizinkan" }
        return listOf("getprop", key)
    }
}

/** Satu-satunya tempat string perintah dibangun. UI dan domain tidak boleh membuat argv sendiri. VERIFY semua di perangkat. */
object GameCommands {
    fun setMode(pkg: String, mode: GameMode): List<String> =
        listOf("cmd", "game", "mode", mode.cmdValue, valid(pkg))

    fun listConfigs(pkg: String): List<String> = listOf("cmd", "game", "list-configs", valid(pkg))

    fun reset(pkg: String): List<String> = listOf("cmd", "game", "reset", valid(pkg))

    /** Perintah baca-saja untuk diagnosa. Sintaks asli ROM dilihat dari keluaran help. */
    fun help(): List<String> = listOf("cmd", "game", "help")

    fun listModes(pkg: String): List<String> = listOf("cmd", "game", "list-modes", valid(pkg))

    private fun valid(pkg: String): String {
        require(PackageName.isValid(pkg)) { "Nama paket tidak valid" }
        return pkg
    }
}
