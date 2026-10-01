package com.intellij.aiplayground.ui.settings.langchain

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.ui.settings.LlmProviderSettingsConfigurable
import com.intellij.aiplayground.ui.settings.LlmSettingsExtensionPoint
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

class LangChainLlmSettingsExtensionPoint(
  private val coroutineScope: CoroutineScope,
) : LlmSettingsExtensionPoint<LangChainProviderSettings> {

  override val supportedType: Class<LangChainProviderSettings>
    get() = LangChainProviderSettings::class.java

  override fun createSettingsConfigurable(project: Project, provider: LlmProvider, settings: LangChainProviderSettings): LlmProviderSettingsConfigurable {
    return LangChainLlmProviderSettingsConfigurable(project, coroutineScope, provider, settings)
  }
}