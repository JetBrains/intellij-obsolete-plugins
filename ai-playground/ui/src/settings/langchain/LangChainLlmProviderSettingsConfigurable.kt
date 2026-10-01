package com.intellij.aiplayground.ui.settings.langchain

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.ui.settings.LlmProviderSettingsConfigurable
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView.Settings
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JComponent

class LangChainLlmProviderSettingsConfigurable(
  project: Project,
  private val coroutineScope: CoroutineScope,
  instance: LlmProvider,
  settings: LangChainProviderSettings,
) : LlmProviderSettingsConfigurable {

  private val viewModel = LangChainLlmProviderSettingsViewModel(project, coroutineScope, instance, settings)

  override fun createComponent(): JComponent {
    return LangChainLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = false))
  }

  override fun createDialogComponent(): JComponent {
    return LangChainLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = true))
  }

  override fun getSettings(): StateFlow<LlmProviderSettings<*>> {
    return viewModel.settings
  }

  override fun dispose() {
    viewModel.dispose()
  }
}