package com.intellij.aidebugger.evaluation.models.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration
import kotlin.io.path.createTempDirectory

class LlmProviderTest {

    @Test
    fun `LlmProviderType fromString handles provider names`() {
        assertEquals(LlmProviderType.OPENAI, LlmProviderType.fromString("openai"))
        assertEquals(LlmProviderType.OPENAI, LlmProviderType.fromString("OPENAI"))
        assertEquals(LlmProviderType.OPENAI, LlmProviderType.fromString("gpt"))
        
        assertEquals(LlmProviderType.ANTHROPIC, LlmProviderType.fromString("anthropic"))
        assertEquals(LlmProviderType.ANTHROPIC, LlmProviderType.fromString("claude"))
        
        assertEquals(LlmProviderType.GEMINI, LlmProviderType.fromString("gemini"))
        assertEquals(LlmProviderType.GEMINI, LlmProviderType.fromString("google"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `LlmProviderType fromString throws on unknown provider`() {
        LlmProviderType.fromString("unknown_provider")
    }

    @Test
    fun `LlmProviderConfig has correct defaults`() {
        val config = LlmProviderConfig()
        
        assertEquals("", config.apiKey)
        assertEquals("gpt-4o-mini", config.modelName)
        assertEquals(0.0, config.temperature)
        assertEquals(Duration.ofMinutes(2), config.timeout)
        assertEquals(LlmProviderType.OPENAI, config.providerType)
    }

    @Test
    fun `LlmProviderConfig accepts custom values`() {
        val config = LlmProviderConfig(
            apiKey = "test-key-123",
            modelName = "gpt-4",
            temperature = 0.7,
            timeout = Duration.ofSeconds(30),
            providerType = LlmProviderType.ANTHROPIC
        )
        
        assertEquals("test-key-123", config.apiKey)
        assertEquals("gpt-4", config.modelName)
        assertEquals(0.7, config.temperature)
        assertEquals(Duration.ofSeconds(30), config.timeout)
        assertEquals(LlmProviderType.ANTHROPIC, config.providerType)
    }

    @Test
    fun `UniversalApiKeyResolver resolves from system env`() {
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            setTestEnv("OPENAI_API_KEY", "system-env-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI)

            assertEquals("system-env-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            } else {
                clearTestEnv("OPENAI_API_KEY")
            }
        }
    }

    @Test
    fun `UniversalApiKeyResolver resolves from dotenv file`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("OPENAI_API_KEY=dotenv-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath)

            assertEquals("dotenv-key", resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver uses fallback env var from dotenv`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("GOOGLE_API_KEY=google-fallback-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.GEMINI, tempDir.absolutePath)

            assertEquals("google-fallback-key", resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver prefers primary over fallback in dotenv`() {
        val tempDir = createTempDirectory().toFile()
        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("""
                GEMINI_API_KEY=gemini-primary
                GOOGLE_API_KEY=google-fallback
            """.trimIndent())

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.GEMINI, tempDir.absolutePath)

            assertEquals("gemini-primary", resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver prioritizes dotenv over system env`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("OPENAI_API_KEY=dotenv-key")
            setTestEnv("OPENAI_API_KEY", "system-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath)

            assertEquals("dotenv-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            } else {
                clearTestEnv("OPENAI_API_KEY")
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver ignores blank keys in dotenv`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("ANTHROPIC_API_KEY")

        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("ANTHROPIC_API_KEY=   ")
            setTestEnv("ANTHROPIC_API_KEY", "system-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.ANTHROPIC, tempDir.absolutePath)

            assertEquals("system-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("ANTHROPIC_API_KEY", originalKey)
            } else {
                clearTestEnv("ANTHROPIC_API_KEY")
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver handles missing dotenv gracefully`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            setTestEnv("OPENAI_API_KEY", "system-env-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath)

            assertEquals("system-env-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            } else {
                clearTestEnv("OPENAI_API_KEY")
            }
            tempDir.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `UniversalApiKeyResolver throws when no key found`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            if (originalKey != null) {
                clearTestEnv("OPENAI_API_KEY")
            }

            UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath)
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver error message includes provider name`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("ANTHROPIC_API_KEY")

        try {
            if (originalKey != null) {
                clearTestEnv("ANTHROPIC_API_KEY")
            }

            try {
                UniversalApiKeyResolver.resolve(LlmProviderType.ANTHROPIC, tempDir.absolutePath)
                fail("Expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("Anthropic"))
                assertTrue(e.message!!.contains("ANTHROPIC_API_KEY"))
            }
        } finally {
            if (originalKey != null) {
                setTestEnv("ANTHROPIC_API_KEY", originalKey)
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver error message includes fallback var`() {
        val tempDir = createTempDirectory().toFile()
        val originalGeminiKey = System.getenv("GEMINI_API_KEY")
        val originalGoogleKey = System.getenv("GOOGLE_API_KEY")

        try {
            if (originalGeminiKey != null) {
                clearTestEnv("GEMINI_API_KEY")
            }
            if (originalGoogleKey != null) {
                clearTestEnv("GOOGLE_API_KEY")
            }

            try {
                UniversalApiKeyResolver.resolve(LlmProviderType.GEMINI, tempDir.absolutePath)
                fail("Expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("GEMINI_API_KEY"))
                assertTrue(e.message!!.contains("GOOGLE_API_KEY"))
            }
        } finally {
            if (originalGeminiKey != null) {
                setTestEnv("GEMINI_API_KEY", originalGeminiKey)
            }
            if (originalGoogleKey != null) {
                setTestEnv("GOOGLE_API_KEY", originalGoogleKey)
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `UniversalApiKeyResolver works without basePath`() {
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            setTestEnv("OPENAI_API_KEY", "system-env-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, null)

            assertEquals("system-env-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            } else {
                clearTestEnv("OPENAI_API_KEY")
            }
        }
    }

    @Test
    fun `UniversalApiKeyResolver handles dotenv read errors`() {
        val originalKey = System.getenv("ANTHROPIC_API_KEY")

        try {
            setTestEnv("ANTHROPIC_API_KEY", "fallback-system-key")

            val resolved = UniversalApiKeyResolver.resolve(LlmProviderType.ANTHROPIC, "/nonexistent/path")

            assertEquals("fallback-system-key", resolved)
        } finally {
            if (originalKey != null) {
                setTestEnv("ANTHROPIC_API_KEY", originalKey)
            } else {
                clearTestEnv("ANTHROPIC_API_KEY")
            }
        }
    }

    @Test
    fun `resolution order is dotenv then system env`() {
        val tempDir = createTempDirectory().toFile()
        val originalKey = System.getenv("OPENAI_API_KEY")

        try {
            val dotEnv = tempDir.resolve(".env")
            dotEnv.writeText("OPENAI_API_KEY=dotenv-key")
            setTestEnv("OPENAI_API_KEY", "system-key")

            assertEquals("dotenv-key", UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath))

            dotEnv.delete()
            assertEquals("system-key", UniversalApiKeyResolver.resolve(LlmProviderType.OPENAI, tempDir.absolutePath))
        } finally {
            if (originalKey != null) {
                setTestEnv("OPENAI_API_KEY", originalKey)
            } else {
                clearTestEnv("OPENAI_API_KEY")
            }
            tempDir.deleteRecursively()
        }
    }

    private fun setTestEnv(name: String, value: String) {
        val env = System.getenv()
        val field = env.javaClass.getDeclaredField("m")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(env) as MutableMap<String, String>
        map[name] = value
    }

    private fun clearTestEnv(name: String) {
        val env = System.getenv()
        val field = env.javaClass.getDeclaredField("m")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(env) as MutableMap<String, String>
        map.remove(name)
    }
}
