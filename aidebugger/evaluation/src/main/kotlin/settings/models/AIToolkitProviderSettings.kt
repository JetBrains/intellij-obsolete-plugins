package com.intellij.aidebugger.evaluation.settings.models

/**
 * Settings interface for AI Toolkit providers (simplified from AI Playground)
 */
interface AIToolkitProviderSettings {
    val displayName: String
    fun updateDisplayName(name: String): AIToolkitProviderSettings
}
