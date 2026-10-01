package com.intellij.aidebugger.evaluation.settings.models

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType

/**
 * Defines a field configuration for a provider
 */
data class ProviderFieldConfig(
    val key: String,
    val displayNameKey: String,
    val commentKey: String,
    val isSecret: Boolean = false,
    val isOptional: Boolean = false,
)

/**
 * Standard field definitions
 */
object StandardProviderFields {
    val API_KEY = ProviderFieldConfig(
        key = "apiKey",
        displayNameKey = "api.key",
        commentKey = "",  // Will be provider-specific
        isSecret = true,
        isOptional = false
    )

    val BASE_URL = ProviderFieldConfig(
        key = "baseUrl",
        displayNameKey = "base.url",
        commentKey = "",  // Will be provider-specific
        isSecret = false,
        isOptional = true
    )
}

/**
 * Provider-specific field configurations
 */
object ProviderFieldConfigurations {

    fun getFieldsForProvider(providerType: LlmProviderType): List<ProviderFieldConfig> {
        return when (providerType) {
            LlmProviderType.OPENAI -> listOf(
                StandardProviderFields.API_KEY.copy(commentKey = "comment.api.key.openai")
            )
            LlmProviderType.OPENAI_COMPATIBLE -> listOf(
                StandardProviderFields.API_KEY.copy(commentKey = "comment.api.key.openai"),
                StandardProviderFields.BASE_URL.copy(commentKey = "comment.base.url.openai")
            )
            LlmProviderType.ANTHROPIC -> listOf(
                StandardProviderFields.API_KEY.copy(commentKey = "comment.api.key.anthropic")
            )
            LlmProviderType.GEMINI -> listOf(
                StandardProviderFields.API_KEY.copy(commentKey = "comment.api.key.gemini")
            )
        }
    }

    fun getDefaultDisplayName(providerType: LlmProviderType): String {
        return when (providerType) {
            LlmProviderType.OPENAI -> "OpenAI"
            LlmProviderType.OPENAI_COMPATIBLE -> "OpenAI-compatible"
            LlmProviderType.ANTHROPIC -> "Anthropic"
            LlmProviderType.GEMINI -> "Google Gemini"
        }
    }
}
