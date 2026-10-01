package com.intellij.aiplayground.models.extension

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.LangChainProviderSettings
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.service

/**
 * Base Extension for providers that use a single API key stored at application level via ApplicationSettingsManagerService
 * Implementors only need to supply provider and service instance.
 */
abstract class BaseApiKeyLlmServiceExtension(
  private val provider: LlmProvider,
) : LlmServiceExtension {

  final override fun getProviderType(): LlmProvider = provider

  abstract override fun createService(): LlmService

  override fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*> {
    val settingsManagerService = service<ApplicationSettingsManagerService>()
    return LangChainProviderSettings(
      enabled = settings.enabled,
      displayName = settings.displayName,
      apiKeyProvider = { settingsManagerService.getProviderApiKey(provider.id) }
    )
  }

  override fun removeSettings(instanceId: LlmProviderInstanceId) {
    service<ApplicationSettingsManagerService>().removeProviderApiKey(provider.id)
  }

  override fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings {
    val settings = instance.settings as LangChainProviderSettings
    settings.apiKey?.let {
      service<ApplicationSettingsManagerService>().storeProviderApiKey(instance.provider.id, it)
    }
    return PlaygroundSettings.ProviderSettings(
      id = instance.id.id,
      providerId = provider.id.id,
      enabled = settings.enabled,
      displayName = settings.displayName,
    )
  }
}
