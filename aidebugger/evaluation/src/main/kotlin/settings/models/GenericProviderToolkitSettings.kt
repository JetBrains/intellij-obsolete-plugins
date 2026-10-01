package com.intellij.aidebugger.evaluation.settings.models

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType

/**
 * Generic provider settings that use a field map to support different provider configurations
 */
data class GenericProviderToolkitSettings(
    override val displayName: String,
    val providerType: LlmProviderType,
    val fields: Map<String, String> = emptyMap()
) : AIToolkitProviderSettings {

    override fun updateDisplayName(name: String) = copy(displayName = name)

    /**
     * Get a field value by key
     */
    fun getField(key: String): String = fields[key] ?: ""

    /**
     * Update a field value
     */
    fun updateField(key: String, value: String): GenericProviderToolkitSettings {
        return copy(fields = fields + (key to value))
    }

    /**
     * Convenience accessors for common fields
     */
    val apiKey: String get() = getField("apiKey")
    val baseUrl: String get() = getField("baseUrl")
}
