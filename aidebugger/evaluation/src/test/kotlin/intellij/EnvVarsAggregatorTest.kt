package com.intellij.aidebugger.evaluation.intellij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class EnvVarsAggregatorTest {

    @Test
    fun `readDotEnv returns empty map when file does not exist`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)
            assertTrue(result.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv parses simple key value pairs`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                KEY1=value1
                KEY2=value2
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals(2, result.size)
            assertEquals("value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv handles export prefix`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                export KEY1=value1
                KEY2=value2
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals(2, result.size)
            assertEquals("value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv strips quotes from values`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                KEY1="value with spaces"
                KEY2='single quoted'
                KEY3=unquoted
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals("value with spaces", result["KEY1"])
            assertEquals("single quoted", result["KEY2"])
            assertEquals("unquoted", result["KEY3"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv ignores comments and empty lines`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                # This is a comment
                KEY1=value1

                KEY2=value2
                # Another comment
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals(2, result.size)
            assertEquals("value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv ignores invalid lines`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                KEY1=value1
                INVALID_NO_EQUALS
                =NO_KEY
                KEY2=value2
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals(2, result.size)
            assertEquals("value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv handles values with equals sign`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                CONNECTION_STRING=Server=localhost;Database=test
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals("Server=localhost;Database=test", result["CONNECTION_STRING"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv handles empty values`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                KEY1=
                KEY2=value2
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals(2, result.size)
            assertEquals("", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv preserves insertion order`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                ZEBRA=z
                ALPHA=a
                BETA=b
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            val keys = result.keys.toList()
            assertEquals(listOf("ZEBRA", "ALPHA", "BETA"), keys)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv handles mixed quote types`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                MIXED1="value"
                MIXED2='value'
                MIXED3="don't"
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals("value", result["MIXED1"])
            assertEquals("value", result["MIXED2"])
            assertEquals("don't", result["MIXED3"])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readDotEnv handles whitespace around values`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = File(tempDir, ".env")
            dotEnv.writeText("""
                  KEY1  =  value1
                KEY2=value2
            """.trimIndent())

            val result = EnvVarsAggregator.readDotEnv(tempDir.absolutePath)

            assertEquals("  value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
