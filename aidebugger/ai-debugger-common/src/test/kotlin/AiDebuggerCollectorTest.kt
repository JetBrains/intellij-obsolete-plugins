package com.intellij.aidebugger.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDebuggerCollectorTest {

    private val regex = Regex(AiDebuggerCollector.VERSION_REGEXP)

    @Test
    fun `accepts semantic versions and dev`() {
        val valid = listOf(
            "0.0.0",
            "1.2.3",
            "10.20.30",
            "999.999.999",
            "dev",
            // leading zeros are allowed by the current regexp
            "01.2.3",
        )
        valid.forEach { v ->
            assertTrue("Expected '$v' to match VERSION_REGEXP", regex.matches(v))
        }
    }

    @Test
    fun `rejects non-conforming versions`() {
        val invalid = listOf(
            "",
            "1",
            "1.2",
            "1.2.3.4",
            "1.2.x",
            "v1.2.3",
            "1.2.3-dev",
            " 1.2.3",
            "1.2.3 ",
            "DEV",
            "1.2.3\n",
        )
        invalid.forEach { v ->
            assertFalse("Expected '$v' NOT to match VERSION_REGEXP", regex.matches(v))
        }
    }
}
