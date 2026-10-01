package com.intellij.aiplayground.ui.settings.aiassistant

import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView
import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineScope

class AIAssistantLlmProviderSettingsView(
  parentScope: CoroutineScope,
  viewModel: AIAssistantLlmProviderSettingsViewModel,
  settings: Settings,
) : LlmProviderSettingsView<AIAssistantLlmProviderSettingsViewModel, LlmProviderSettingsView.Settings>(parentScope, viewModel, settings), Disposable