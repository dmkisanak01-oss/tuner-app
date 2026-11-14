package com.example.tuner.system

/** Mode Game Mode API. Nilai cmd diambil dari enum, tidak pernah dari input mentah. */
enum class GameMode(val cmdValue: String) {
    STANDARD("standard"),
    PERFORMANCE("performance"),
    BATTERY("battery"),
    CUSTOM("custom"), // downscale + FPS lewat `cmd game set`
}

/** Hasil `cmd game list-modes`: mode aktif dan mode yang diumumkan game. */
data class ModeInfo(val current: GameMode?, val available: Set<GameMode>)

object ModeParser {
    private val CURRENT = Regex("current mode:\\s*([a-z]+)")
    private val AVAILABLE = Regex("available game modes:\\s*\\[([^\\]]*)\\]")

    /** Contoh nyata (Android 15, Xiaomi): "com.x current mode: standard, available game modes: [standard,custom]". */
    fun parse(output: String): ModeInfo? {
        val available = AVAILABLE.find(output)?.groupValues?.get(1) ?: return null
        val modes = available.split(',').mapNotNull { name -> GameMode.entries.find { it.cmdValue == name.trim() } }.toSet()
        val current = CURRENT.find(output)?.groupValues?.get(1)?.let { n -> GameMode.entries.find { it.cmdValue == n } }
        return ModeInfo(current, modes)
    }
}

/** Renderer HWUI global (`debug.hwui.renderer`). Hanya memengaruhi app yang merender lewat HWUI; hilang saat reboot. */
enum class Renderer(val propValue: String) {
    DEFAULT("default"),
    SKIAGL("skiagl"),
    SKIAVK("skiavk");

    companion object {
        fun fromProp(value: String): Renderer = entries.find { it.propValue == value.trim() } ?: DEFAULT
    }
}

object PipelineParser {
    private val LINE = Regex("Pipeline=([^\\r\\n]+)")

    /** Contoh baris dari `dumpsys gfxinfo <pkg>`: "Pipeline=Skia (OpenGL)". */
    fun parse(output: String): String? = LINE.find(output)?.groupValues?.get(1)?.trim()
}

object RendererCommands {
    fun get(): List<String> = listOf("getprop", "debug.hwui.renderer")
    fun set(renderer: Renderer): List<String> = listOf("setprop", "debug.hwui.renderer", renderer.propValue)
    fun gfxinfo(pkg: String): List<String> {
        require(PackageName.isValid(pkg)) { "Nama paket tidak valid" }
        return listOf("dumpsys", "gfxinfo", pkg)
    }
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

    /** Nilai downscale yang diterima `cmd game set` di ROM (dari keluaran `cmd game help`). */
    val DOWNSCALE_VALUES: List<String> = listOf("disable") +
        listOf("0.3", "0.35", "0.4", "0.45", "0.5", "0.55", "0.6", "0.65", "0.7", "0.75", "0.8", "0.85", "0.9")

    /** `downscale` harus salah satu nilai di atas. `fps` 0 = bawaan. */
    fun setCustom(pkg: String, downscale: String, fps: Int): List<String> {
        require(downscale in DOWNSCALE_VALUES) { "Nilai downscale tidak valid" }
        require(fps in 0..240) { "FPS di luar rentang" }
        return listOf("cmd", "game", "set", "--downscale", downscale, "--fps", fps.toString(), valid(pkg))
    }

    fun listModes(pkg: String): List<String> = listOf("cmd", "game", "list-modes", valid(pkg))

    private fun valid(pkg: String): String {
        require(PackageName.isValid(pkg)) { "Nama paket tidak valid" }
        return pkg
    }
}
