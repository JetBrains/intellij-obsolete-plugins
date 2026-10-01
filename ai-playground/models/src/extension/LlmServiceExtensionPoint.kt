package com.intellij.aiplayground.models.extension

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmService
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Interface for LLM service providers
 * Implement this to register new LLM providers to the system
 */
interface LlmServiceExtension {
  /**
   * Gets the provider type for this service extension
   */
  fun getProviderType(): LlmProvider

  /**
   * Creates an LLM service for this provider
   */
  fun createService(): LlmService

  fun createSettings(settings: PlaygroundSettings.ProviderSettings): LlmProviderSettings<*>
  fun updateSettings(instance: LlmProviderInstance): PlaygroundSettings.ProviderSettings
  fun removeSettings(instanceId: LlmProviderInstanceId)
  fun isAvailable(project: Project): StateFlow<Boolean> = MutableStateFlow(true)

  companion object {
    val EP_NAME: ExtensionPointName<LlmServiceExtension> = ExtensionPointName<LlmServiceExtension>("com.intellij.aiplayground.llmServiceProvider")

    /**
     * Gets all registered LLM service extensions
     */
    fun getAllExtensions(): List<LlmServiceExtension> {
      return EP_NAME.extensionList
    }

    fun getAllRegisteredProviders(): List<LlmProvider> {
      return getAllExtensions().map { it.getProviderType() }
    }

    /**
     * Gets all registered LLM providers
     */
    fun getAllAvailableProviders(project: Project): StateFlow<List<LlmProvider>> {
      return project.service<LlmServiceExtensionService>().allAvailableProviders
    }

    fun getProviderExtension(provider: LlmProvider): LlmServiceExtension = getAllExtensions()
      .first { it.getProviderType() == provider }

    /**
     * Gets a provider by ID if it was registered. Does not check if it is available
     */
    fun getProviderById(id: LlmProviderId): LlmProvider? {
      return getAllRegisteredProviders().find { it.id == id }
    }

    @Service(Service.Level.PROJECT)
    private class LlmServiceExtensionService(private val project: Project, private val coroutineScope: CoroutineScope) {
      // service is needed to cache StateFlow per project instead of creating a new one every time
      val allAvailableProviders: StateFlow<List<LlmProvider>> by lazy {
        combine(
          getAllExtensions().map { extension ->
            extension.isAvailable(project).map { extension to it }
          }
        ) { it }.map {
          it.filter { (_, available) -> available }
            .map { (extension, _) -> extension.getProviderType() }
            .distinctBy { provider -> provider.id }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())
      }
    }
  }
} 