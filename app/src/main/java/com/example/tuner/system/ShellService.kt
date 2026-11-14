package com.example.tuner.system

import kotlin.system.exitProcess

/** Dijalankan Shizuku di proses shell (bukan di proses app). Dibuat lewat konstruktor tanpa argumen. */
class ShellService : IShellService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(argv: Array<String>): String {
        return try {
            val process = ProcessBuilder(argv.toList()).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            "${process.waitFor()}\n$output"
        } catch (e: Exception) {
            "-1\n${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
