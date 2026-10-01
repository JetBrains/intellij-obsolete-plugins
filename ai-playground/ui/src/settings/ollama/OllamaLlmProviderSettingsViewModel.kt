package com.intellij.aiplayground.ui.settings.ollama

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.OllamaProviderSettings
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsViewModel
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update

class OllamaLlmProviderSettingsViewModel(
  project: Project,
  parentScope: CoroutineScope,
  provider: LlmProvider,
  providerSettings: OllamaProviderSettings,
) : LlmProviderSettingsViewModel<OllamaProviderSettings>(project, parentScope, provider, providerSettings) {

  fun updateEndpoint(endpoint: String) {
    _settings.update {
      it.copy(endpoint = endpoint)
    }
  }
}