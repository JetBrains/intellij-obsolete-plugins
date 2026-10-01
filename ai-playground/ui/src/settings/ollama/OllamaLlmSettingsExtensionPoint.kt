package com.intellij.aiplayground.ui.settings.ollama

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.OllamaProviderSettings
import com.intellij.aiplayground.ui.settings.LlmProviderSettingsConfigurable
import com.intellij.aiplayground.ui.settings.LlmSettingsExtensionPoint
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsView.Settings
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JComponent

class OllamaLlmSettingsExtensionPoint(
  private val coroutineScope: CoroutineScope,
) : LlmSettingsExtensionPoint<OllamaProviderSettings> {

  override val supportedType: Class<OllamaProviderSettings>
    get() = OllamaProviderSettings::class.java

  override fun createSettingsConfigurable(project: Project, provider: LlmProvider, settings: OllamaProviderSettings): LlmProviderSettingsConfigurable {
    val viewModel = OllamaLlmProviderSettingsViewModel(project, coroutineScope, provider, settings)
    return object : LlmProviderSettingsConfigurable {
      override fun dispose() {
        viewModel.dispose()
      }

      override fun createComponent(): JComponent {
        return OllamaLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = false))
      }

      override fun createDialogComponent(): JComponent {
        return OllamaLlmProviderSettingsView(coroutineScope, viewModel, Settings(dialog = true))
      }

      override fun getSettings(): StateFlow<LlmProviderSettings<*>> {
        return viewModel.settings
      }
    }
  }
}