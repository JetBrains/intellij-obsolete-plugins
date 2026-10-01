package com.intellij.aiplayground.ui.settings

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class LlmSettingsManager(
  private val project: Project,
) {

  fun createProviderSettingsConfigurable(provider: LlmProvider, settings: LlmProviderSettings<*>): LlmProviderSettingsConfigurable {
    val extension = LlmSettingsExtensionPoint.getProviderExtension(settings::class) as LlmSettingsExtensionPoint<LlmProviderSettings<*>>
    return extension.createSettingsConfigurable(project, provider, settings)
  }

  companion object {
    fun getInstance(project: Project): LlmSettingsManager = project.service<LlmSettingsManager>()
  }
}