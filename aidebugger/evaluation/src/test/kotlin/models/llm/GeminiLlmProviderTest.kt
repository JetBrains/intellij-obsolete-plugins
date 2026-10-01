package com.intellij.aidebugger.evaluation.models.llm

import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Duration

class GeminiLlmProviderTest {

    @Test
    fun `create factory method builds provider with config`() {
        val config = LlmProviderConfig(
            apiKey = "test-key-123",
            modelName = "gemini-1.5-pro",
            temperature = 0.5,
            timeout = Duration.ofSeconds(30)
        )

        val provider = GeminiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create uses config model name`() {
        val config = LlmProviderConfig(
            apiKey = "test-key",
            modelName = "gemini-1.5-flash",
            temperature = 0.0,
            timeout = Duration.ofMinutes(1)
        )

        val provider = GeminiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create uses config temperature`() {
        val config = LlmProviderConfig(
            apiKey = "test-key",
            modelName = "gemini-1.5-pro",
            temperature = 0.9,
            timeout = Duration.ofSeconds(45)
        )

        val provider = GeminiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with null temperature`() {
        val config = LlmProviderConfig(
            apiKey = "test-key",
            modelName = "gemini-1.5-flash",
            temperature = null,
            timeout = Duration.ofMinutes(2)
        )

        val provider = GeminiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with different response class`() {
        data class CustomResponse(val value: String)

        val config = LlmProviderConfig(
            apiKey = "test-key",
            modelName = "gemini-1.5-pro",
            temperature = 0.0,
            timeout = Duration.ofMinutes(2)
        )

        val provider = GeminiLlmProvider.create<CustomResponse>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with timeout configuration`() {
        val config = LlmProviderConfig(
            apiKey = "test-key",
            modelName = "gemini-1.5-pro",
            temperature = 0.0,
            timeout = Duration.ofSeconds(15)
        )

        val provider = GeminiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }
}
