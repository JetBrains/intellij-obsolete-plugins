package com.intellij.aiplayground.models

import com.intellij.aiplayground.models.chat.ChatResponseEvent
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.extension.LlmServiceExtension.Companion.getProviderExtension
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings.Companion.PLAYGROUND_SETTINGS_UPDATES_TOPIC
import com.intellij.aiplayground.models.settings.PlaygroundSettings.PlaygroundSettingsUpdatesListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class LlmServiceManager(private val project: Project, coroutineScope: CoroutineScope) : Disposable {
  val availableProvidersState: StateFlow<List<LlmProvider>> = LlmServiceExtension.getAllAvailableProviders(project)

  val configuredProviders: List<LlmProviderInstance>
    get() {
      val playgroundSettings = service<PlaygroundSettings>()
      val configuredProviders = playgroundSettings.getConfiguredProviders()
      return configuredProviders.filter { it.provider in availableProvidersState.value }
    }

  private val _configuredProvidersState = MutableStateFlow(configuredProviders)
  val configuredProvidersState: StateFlow<List<LlmProviderInstance>> = _configuredProvidersState.asStateFlow()

  init {
    coroutineScope.launch {
      availableProvidersState.collect { availableProviders ->
        _configuredProvidersState.value = configuredProviders

        val registeredProviders = LlmServiceExtension.getAllRegisteredProviders()

        val playgroundSettings = service<PlaygroundSettings>()
        val configuredProviders = playgroundSettings.getConfiguredProviders()
          .groupBy { it.provider.id }

        registeredProviders.forEach { provider ->
          if (provider.predefined) {
            val configurations = configuredProviders[provider.id]
            if (provider in availableProviders) {
              if (configurations == null) {
                val newInstance = LlmProviderInstance(
                  provider = provider,
                  settings = playgroundSettings.createProviderSettings(provider)
                )
                playgroundSettings.addProviderInstanceInBeginning(newInstance)
              }
              else if (configurations.size > 1 && !provider.canHaveMultipleInstances) {
                configurations.drop(1).forEach { configuration ->
                  playgroundSettings.removeProviderInstance(configuration.id)
                }
              }
            }
            else {
              //configurations?.forEach { instance -> playgroundSettings.removeProviderInstance(instance.id) }
            }
          }
        }
      }
    }

    ApplicationManager.getApplication().messageBus.connect(this).subscribe(PLAYGROUND_SETTINGS_UPDATES_TOPIC, object : PlaygroundSettingsUpdatesListener {
      override fun settingsUpdated() {
        _configuredProvidersState.value = configuredProviders
      }
    })
  }

  suspend fun getModel(instance: LlmProviderInstance, modelId: LlmModelId): LlmModel {
    val model = LlmServiceExtension.getAllExtensions()
      .first { it.getProviderType().id == instance.provider.id }
      .createService().getSupportedModels(project, instance.settings)
      .first { it.id == modelId }
    return model
  }

  fun getSupportedModels(instance: LlmProviderInstance): Flow<List<LlmModel>> {
    // TODO doesn't actually emits flow of the "updating list of llmmodel-s": only emits it once
    // so if the list of models on LLM provider side changed, the flow subscriber won't see it
    // could be solved by a periodic update (e.g. once every 60sec)
    if (!instance.settings.enabled) {
      return flow { emit(emptyList()) }
    }
    return flow {
      emit(
        getProviderService(instance.provider)
          .getSupportedModels(project, instance.settings)
      )
    }
  }

  suspend fun streamingComplete(
    model: LlmModel,
    prompt: String,
    context: RequestContext,
    requestConfig: ChatRequestConfig,
    instance: LlmProviderInstance,
  ): Flow<ChatResponseEvent> {
    val providerSettings = instance.settings
    return getProviderService(model.provider)
      .streamingComplete(project, prompt, context.systemPrompt, model, providerSettings, requestConfig, context.messages)
  }

  fun getProviderService(provider: LlmProvider): LlmService = getProviderExtension(provider)
    .createService()

  fun addProviderInstance(instance: LlmProviderInstance) {
    service<PlaygroundSettings>().addProviderInstance(instance)
  }

  fun updateProviderSettings(settings: List<LlmProviderInstance>) {
    service<PlaygroundSettings>().updateProviderSettings(settings)
  }

  suspend fun testConnection(provider: LlmProvider, providerSettings: LlmProviderSettings<*>) {
    getProviderService(provider)
      .testConnection(project, providerSettings)
  }

  fun createProviderSettings(provider: LlmProvider): LlmProviderSettings<*> {
    return service<PlaygroundSettings>().createProviderSettings(provider)
  }

  suspend fun removeProviderApiKeyIfNoProviders(provider: LlmProvider) {
    val configuredProviders = configuredProvidersState.first()
    val hasProviderConfigured = configuredProviders.any { configuredProvider ->
      configuredProvider.provider.id == provider.id
    }
    if (!hasProviderConfigured) {
      service<ApplicationSettingsManagerService>().removeProviderApiKey(provider.id)
    }
  }

  override fun dispose() {
  }

  companion object {
    fun getInstance(project: Project): LlmServiceManager = project.service<LlmServiceManager>()
  }
}
