package com.intellij.aiplayground.ui.settings.openai

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.OpenAIProviderSettings
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsViewModel
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update

class OpenAILlmProviderSettingsViewModel(
  project: Project,
  parentScope: CoroutineScope,
  provider: LlmProvider,
  providerSettings: OpenAIProviderSettings,
) : LlmProviderSettingsViewModel<OpenAIProviderSettings>(project, parentScope, provider, providerSettings) {

  fun updateApiKey(apiKey: String) {
    _settings.update {
      it.copy(apiKeyProvider = { apiKey })
    }
  }

  fun updateBaseUrl(baseUrl: String) {
    _settings.update {
      it.copy(endpoint = baseUrl)
    }
  }
}