package com.intellij.aiplayground.ui.settings.aiassistant

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.AIAssistantProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.ui.settings.LlmProviderSettingsConfigurable
import com.intellij.aiplayground.ui.settings.LlmSettingsExtensionPoint
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView.Settings
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JComponent

class AIAssistantLlmSettingsExtensionPoint(
  private val coroutineScope: CoroutineScope,
) : LlmSettingsExtensionPoint<AIAssistantProviderSettings> {

  override val supportedType: Class<AIAssistantProviderSettings>
    get() = AIAssistantProviderSettings::class.java

  override fun createSettingsConfigurable(project: Project, provider: LlmProvider, settings: AIAssistantProviderSettings): LlmProviderSettingsConfigurable {
    val viewModel = AIAssistantLlmProviderSettingsViewModel(project, coroutineScope, provider, settings)
    return object : LlmProviderSettingsConfigurable {
      override fun dispose() {
        viewModel.dispose()
      }

      override fun createComponent(): JComponent {
        return AIAssistantLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = false))
      }

      override fun createDialogComponent(): JComponent {
        return AIAssistantLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = true))
      }

      override fun getSettings(): StateFlow<LlmProviderSettings<*>> {
        return viewModel.settings
      }
    }
  }
}