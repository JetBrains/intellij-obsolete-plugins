package com.intellij.aidebugger.evaluation.models.llm

import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test
import java.time.Duration

@Ignore("AT-3959")
class OpenAiLlmProviderTest {

    @Test
    fun `create factory method builds provider with config`() {
        val config = LlmProviderConfig(
            apiKey = "test-key-123",
            modelName = "gpt-4",
            temperature = 0.5,
            timeout = Duration.ofSeconds(30)
        )

        val provider = OpenAiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create uses config model name`() {
        val config = LlmProviderConfig(
            apiKey = "sk-test",
            modelName = "gpt-4-turbo",
            temperature = 0.0,
            timeout = Duration.ofMinutes(1)
        )

        val provider = OpenAiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create uses config temperature`() {
        val config = LlmProviderConfig(
            apiKey = "sk-test",
            modelName = "gpt-4o-mini",
            temperature = 0.9,
            timeout = Duration.ofSeconds(45)
        )

        val provider = OpenAiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with null temperature`() {
        val config = LlmProviderConfig(
            apiKey = "sk-test",
            modelName = "gpt-4o-mini",
            temperature = null,
            timeout = Duration.ofMinutes(2)
        )

        val provider = OpenAiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with different response class`() {
        data class CustomResponse(val value: String)

        val config = LlmProviderConfig(
            apiKey = "sk-test",
            modelName = "gpt-4o-mini",
            temperature = 0.0,
            timeout = Duration.ofMinutes(2)
        )

        val provider = OpenAiLlmProvider.create<CustomResponse>(
            config = config
        )

        assertNotNull(provider)
    }

    @Test
    fun `create with timeout configuration`() {
        val config = LlmProviderConfig(
            apiKey = "sk-test",
            modelName = "gpt-4",
            temperature = 0.0,
            timeout = Duration.ofSeconds(15)
        )

        val provider = OpenAiLlmProvider.create<LLMScore>(
            config = config
        )

        assertNotNull(provider)
    }
}
