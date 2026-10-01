package com.intellij.aidebugger.evaluation.settings.models

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.openapi.util.NlsSafe
import java.util.UUID

/**
 * Represents a single instance of an LLM provider with credentials.
 * Allows multiple instances per provider type (e.g., "OpenAI Prod", "OpenAI Dev").
 */
data class ProviderInstance(
    val id: String = UUID.randomUUID().toString(),
    val providerType: LlmProviderType,
    val name: String,
    val apiKey: String = "",
    val baseUrl: String = "",
    val disabledModels: Set<String> = emptySet(),  // Model IDs that are disabled by user
    val availableModels: List<LlmModel> = emptyList(),  // Cached list of available models
) {
    /**
     * Check if this instance has valid credentials
     */
    fun hasCredentials(): Boolean = apiKey.isNotBlank()

    /**
     * Get display name with provider type
     */
    @NlsSafe
    fun getDisplayName(): String = "$name (${providerType.name})"

    /**
     * Check if a model is enabled
     */
    fun isModelEnabled(modelId: String): Boolean = !disabledModels.contains(modelId)
}
