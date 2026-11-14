package com.example.tuner.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandsTest {
    @Test fun validPackagesAccepted() {
        assertTrue(PackageName.isValid("com.example.game"))
        assertTrue(PackageName.isValid("com.tencent.ig"))
        assertTrue(PackageName.isValid("a.b_c.d9"))
    }

    @Test fun injectionAndMalformedPackagesRejected() {
        listOf(
            "", "game", "com.example.game; rm -rf /", "com.example.game && id", "com.example.game|id",
            "com.example.game\nid", "com.example. game", "com.example.\$(id)", "com.example.`id`",
            "1com.example", "com..example", ".com.example", "com.example.", "../../x.y", "com.example.game'",
        ).forEach { assertFalse("harus ditolak: $it", PackageName.isValid(it)) }
    }

    @Test fun setModeBuildsExpectedArgv() {
        assertEquals(
            listOf("cmd", "game", "mode", "performance", "com.example.game"),
            GameCommands.setMode("com.example.game", GameMode.PERFORMANCE),
        )
    }

    @Test fun commandsRefuseBadPackage() {
        assertThrows(IllegalArgumentException::class.java) { GameCommands.reset("x; id") }
        assertThrows(IllegalArgumentException::class.java) { GameCommands.listConfigs("") }
    }

    @Test fun safeArgsRejectsShellMetacharacters() {
        SafeArgs.check(listOf("cmd", "game", "mode", "battery", "com.example.game"))
        listOf("a b", "a;b", "a|b", "a&b", "\$(id)", "`id`", "a\nb", "a>b", "a'b", "a\"b", "").forEach {
            assertThrows("harus ditolak: [$it]", IllegalArgumentException::class.java) { SafeArgs.check(listOf("cmd", it)) }
        }
        assertThrows(IllegalArgumentException::class.java) { SafeArgs.check(emptyList()) }
    }
}

class ModeParserTest {
    @Test fun parsesRealMobileLegendsOutput() {
        // Keluaran asli dari Xiaomi 2312DRAABG, Android 15.
        val info = ModeParser.parse("com.mobile.legends current mode: standard, available game modes: [standard,custom]")!!
        assertEquals(GameMode.STANDARD, info.current)
        assertEquals(setOf(GameMode.STANDARD, GameMode.CUSTOM), info.available)
    }

    @Test fun unknownOutputReturnsNull() {
        assertEquals(null, ModeParser.parse("Error: whatever"))
    }

    @Test fun setCustomBuildsArgvAndValidates() {
        assertEquals(
            listOf("cmd", "game", "set", "--downscale", "0.7", "--fps", "60", "com.example.game"),
            GameCommands.setCustom("com.example.game", "0.7", 60),
        )
        assertThrows(IllegalArgumentException::class.java) { GameCommands.setCustom("com.example.game", "0.1", 60) }
        assertThrows(IllegalArgumentException::class.java) { GameCommands.setCustom("com.example.game", "disable", 999) }
        assertThrows(IllegalArgumentException::class.java) { GameCommands.setCustom("bad pkg", "disable", 0) }
    }
}

class RendererTest {
    @Test fun parsesPropValues() {
        assertEquals(Renderer.SKIAVK, Renderer.fromProp("skiavk\n"))
        assertEquals(Renderer.SKIAGL, Renderer.fromProp(" skiagl "))
        assertEquals(Renderer.DEFAULT, Renderer.fromProp(""))
        assertEquals(Renderer.DEFAULT, Renderer.fromProp("whatever"))
    }

    @Test fun buildsSafeCommands() {
        assertEquals(listOf("setprop", "debug.hwui.renderer", "skiavk"), RendererCommands.set(Renderer.SKIAVK))
        SafeArgs.check(RendererCommands.set(Renderer.DEFAULT))
        assertThrows(IllegalArgumentException::class.java) { RendererCommands.gfxinfo("x; id") }
    }

    @Test fun parsesPipelineLine() {
        assertEquals("Skia (Vulkan)", PipelineParser.parse("Stats since: 1\nPipeline=Skia (Vulkan)\nTotal frames"))
        assertEquals(null, PipelineParser.parse("no pipeline here"))
    }
}
