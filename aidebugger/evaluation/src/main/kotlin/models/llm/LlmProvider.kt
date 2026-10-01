package com.intellij.aidebugger.evaluation.models.llm

import com.intellij.aidebugger.evaluation.intellij.EnvVarsAggregator
import com.intellij.openapi.util.NlsSafe
import java.time.Duration

/**
 * Factory function to create an LLM provider based on the provider type in the config.
 *
 * @param config The configuration containing provider type, API key, model name, etc.
 * @return An instance of the appropriate LlmProvider implementation
 */
inline fun <reified T : Any> createLlmProvider(config: LlmProviderConfig): LlmProvider<T> {
    return when (config.providerType) {
        LlmProviderType.OPENAI,
        LlmProviderType.OPENAI_COMPATIBLE -> OpenAiLlmProvider.create<T>(config)

        LlmProviderType.ANTHROPIC -> AnthropicLlmProvider.create<T>(config)

        LlmProviderType.GEMINI -> GeminiLlmProvider.create<T>(config)
    }
}

interface LlmProvider<T> {
    suspend fun complete(prompt: String): T
}

/**
 * Enum representing supported LLM providers with their API key configuration.
 * Each provider knows its environment variable name, fallback options, and display name.
 */
enum class LlmProviderType(
  val envVarName: String,
  val fallbackEnvVarName: String? = null,
  @get:NlsSafe val displayName: String = envVarName.substringBefore("_API_KEY")
        .lowercase()
        .replaceFirstChar { it.uppercase() },
  val supportsBaseUrl: Boolean = false,
  val defaultModel: String
) {
    OPENAI(
        envVarName = "OPENAI_API_KEY",
        displayName = "OpenAI",
        supportsBaseUrl = false,
        defaultModel = "gpt-4o"
    ),

    OPENAI_COMPATIBLE(
        envVarName = "OPENAI_API_KEY",
        displayName = "OpenAI-compatible",
        supportsBaseUrl = true,
        defaultModel = "gpt-4o"
    ),

    ANTHROPIC(
        envVarName = "ANTHROPIC_API_KEY",
        displayName = "Anthropic",
        supportsBaseUrl = false,
        defaultModel = "claude-3-5-sonnet-20241022"
    ),

    GEMINI(
        envVarName = "GEMINI_API_KEY",
        fallbackEnvVarName = "GOOGLE_API_KEY",
        displayName = "Gemini",
        supportsBaseUrl = false,
        defaultModel = "gemini-2.0-flash-exp"
    );

    companion object {
        fun fromString(value: String): LlmProviderType {
            return when (value.lowercase()) {
                "openai", "gpt" -> OPENAI
                "openai_compatible", "openai-compatible" -> OPENAI_COMPATIBLE
                "anthropic", "claude" -> ANTHROPIC
                "gemini", "google" -> GEMINI
                else -> valueOf(value.uppercase())
            }
        }
    }
}

data class LlmProviderConfig(
    val apiKey: String = "",
    val modelName: String = "gpt-4o-mini",
    val temperature: Double? = 0.0,
    val timeout: Duration = Duration.ofMinutes(2),
    val providerType: LlmProviderType = LlmProviderType.OPENAI
)

/**
 * Universal API key resolver for all LLM providers.
 * Resolution order:
 * - .env file in base path
 * - System environment variable
 * - Fallback environment variable (if configured)
 */
object UniversalApiKeyResolver {
    fun resolve(
        providerType: LlmProviderType,
        basePath: String? = null
    ): String {
        if (!basePath.isNullOrBlank()) {
            runCatching {
                val envVars = EnvVarsAggregator.readDotEnv(basePath)

                envVars[providerType.envVarName]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }

                providerType.fallbackEnvVarName?.let { fallback ->
                    envVars[fallback]?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()?.let { return it }
        }

        System.getenv(providerType.envVarName)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        providerType.fallbackEnvVarName?.let { fallback ->
            System.getenv(fallback)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        val varNames = listOfNotNull(
            providerType.envVarName,
            providerType.fallbackEnvVarName
        ).joinToString(" or ")

        throw IllegalArgumentException(
            "Missing ${providerType.displayName} API key. " +
                "Set apiKey in config, provide .env with $varNames, " +
                "set $varNames environment variable, or configure in IDE Settings > AI Agents Debugger."
        )
    }
}
