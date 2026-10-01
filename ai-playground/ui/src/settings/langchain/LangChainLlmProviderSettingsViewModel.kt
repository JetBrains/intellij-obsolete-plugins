package com.intellij.aiplayground.ui.settings.langchain

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsViewModel
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update

class LangChainLlmProviderSettingsViewModel(
  project: Project,
  parentScope: CoroutineScope,
  provider: LlmProvider,
  providerSettings: LangChainProviderSettings,
) : LlmProviderSettingsViewModel<LangChainProviderSettings>(project, parentScope, provider, providerSettings) {

  fun updateApiKey(apiKey: String) {
    _settings.update {
      it.copy(apiKeyProvider = { apiKey } )
    }
  }
}