package com.intellij.aiplayground.openai

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.OPENAI_COMPATIBLE_PROVIDER
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.OpenAIProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.service

class OpenAICompatibleProviderExtension : LlmServiceExtension {
  override fun getProviderType(): LlmProvider = OPENAI_COMPATIBLE_PROVIDER

  override fun createService(): LangChainOpenAiService = LangChainOpenAiService()

  override fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*> {
    return OpenAIProviderSettings(
      enabled = settings.enabled,
      displayName = settings.displayName,
      apiKeyProvider = { null },
      endpoint = settings.properties["endpoint"]
    )
  }

  override fun removeSettings(instanceId: LlmProviderInstanceId) {
    service<ApplicationSettingsManagerService>().removeInstanceApiKey(instanceId)
  }

  override fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings {
    val settings = instance.settings as OpenAIProviderSettings
    settings.apiKey?.let { service<ApplicationSettingsManagerService>().storeInstanceApiKey(instance.id, it) }
    return PlaygroundSettings.ProviderSettings(
      id = instance.id.id,
      providerId = OPENAI_COMPATIBLE_PROVIDER.id.id,
      enabled = settings.enabled,
      displayName = settings.displayName,
      properties = buildMap {
        settings.endpoint?.let { put("endpoint", it) }
      }
    )
  }
}