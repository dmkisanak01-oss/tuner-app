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
