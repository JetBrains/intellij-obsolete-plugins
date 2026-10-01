package com.intellij.aiplayground.ui.settings.openai

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.OPENAI_COMPATIBLE_PROVIDER
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.OpenAIProviderSettings
import com.intellij.aiplayground.ui.settings.LlmProviderSettingsConfigurable
import com.intellij.aiplayground.ui.settings.LlmSettingsExtensionPoint
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JComponent

class OpenAILlmSettingsExtensionPoint(
  private val coroutineScope: CoroutineScope,
) : LlmSettingsExtensionPoint<OpenAIProviderSettings> {

  override val supportedType: Class<OpenAIProviderSettings>
    get() = OpenAIProviderSettings::class.java

  override fun createSettingsConfigurable(project: Project, provider: LlmProvider, settings: OpenAIProviderSettings): LlmProviderSettingsConfigurable {
    val viewModel = OpenAILlmProviderSettingsViewModel(project, coroutineScope, provider, settings)
    return object : LlmProviderSettingsConfigurable {
      override fun dispose() {
        viewModel.dispose()
      }

      override fun createComponent(): JComponent {
        return OpenAILlmProviderSettingsView(
          coroutineScope,
          viewModel,
          OpenAILlmProviderSettingsView.Settings(dialog = false, endpoint = provider == OPENAI_COMPATIBLE_PROVIDER)
        )
      }

      override fun createDialogComponent(): JComponent {
        return OpenAILlmProviderSettingsView(
          coroutineScope,
          viewModel,
          OpenAILlmProviderSettingsView.Settings(dialog = true, endpoint = provider == OPENAI_COMPATIBLE_PROVIDER)
        )
      }

      override fun getSettings(): StateFlow<LlmProviderSettings<*>> {
        return viewModel.settings
      }
    }
  }
}