package com.intellij.aiplayground.ui.settings.aiassistant

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.AIAssistantProviderSettings
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsViewModel
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

class AIAssistantLlmProviderSettingsViewModel(
  project: Project,
  parentScope: CoroutineScope,
  provider: LlmProvider,
  providerSettings: AIAssistantProviderSettings,
) : LlmProviderSettingsViewModel<AIAssistantProviderSettings>(project, parentScope, provider, providerSettings) {
}