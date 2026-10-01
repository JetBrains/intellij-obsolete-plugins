package com.intellij.aiplayground.ui.settings

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.ui.env.ApiKeysInEnvVariablesService
import com.intellij.aiplayground.ui.settings.SmartListModel.Companion.AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LlmSettingsViewModel(private val project: Project, parentScope: CoroutineScope) {

  private val coroutineScope = parentScope.childScope("LlmSettingsViewModel")

  private val _providers = MutableStateFlow<List<LlmProviderInstance>>(emptyList())
  val providers: StateFlow<List<LlmProviderInstance>> = _providers.asStateFlow()

  private val _selectedProvider = MutableStateFlow<LlmProviderInstance?>(null)
  val selectedProvider: StateFlow<LlmProviderInstance?> = _selectedProvider.asStateFlow()

  private val _providerConfigurable = MutableStateFlow<LlmProviderSettingsConfigurable?>(null)
  val providerConfigurable: StateFlow<LlmProviderSettingsConfigurable?> = _providerConfigurable.asStateFlow()

  private val _showAPIKeysFoundBanner = MutableStateFlow(
    PropertiesComponent.getInstance(project).getBoolean(AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS, true)
  )
  val showAPIKeysFoundBanner: StateFlow<Boolean> = _showAPIKeysFoundBanner.asStateFlow()

  val newApiKeysNotInCurrentProviders: StateFlow<Map<LlmProviderId, ApiKeysInEnvVariablesService.FoundKey>> = combine(
    providers,
    ApiKeysInEnvVariablesService.getInstance(project).newKeys
  ) { providers, newKeys ->

    newKeys.filter { !providers.any { provider -> provider.provider.id == it.key } }
  }.stateIn(coroutineScope, SharingStarted.Eagerly, mapOf())

  private var saved: Map<LlmProviderInstanceId, LlmProviderInstance>? = null
  private var resetJob: Job? = null
  private var updateJob: Job? = null

  init {
    reset()
  }

  fun reset() {
    val oldJob = resetJob
    resetJob = coroutineScope.launch {
      oldJob?.cancelAndJoin()
      LlmServiceManager.getInstance(project).configuredProvidersState
        .onEach { instances ->
          _providers.value = instances
          saved = instances.associateBy { it.id }
        }
        .first()
    }
  }

  fun apply() {
    val updated = _providers.value
    LlmServiceManager.getInstance(project).updateProviderSettings(updated)
    saved = updated.associateBy { it.id }
  }

  /** Fetches new providers from persistent settings without removing the unsaved ones */
  fun update() {
    val oldJob = updateJob
    updateJob = coroutineScope.launch {
      oldJob?.cancelAndJoin()
      LlmServiceManager.getInstance(project).configuredProvidersState
        .onEach { instances ->
          val mergedProviders = mutableListOf<LlmProviderInstance>()
          mergedProviders.addAll(_providers.value)
          instances.filter {
            (it.provider.canHaveMultipleInstances && !mergedProviders.any { instance -> instance.id == it.id })
            || (!it.provider.canHaveMultipleInstances && !mergedProviders.any { instance -> instance.provider.id == it.provider.id })
          }.let { mergedProviders.addAll(it) }
          _providers.value = mergedProviders
          saved = mergedProviders.associateBy { it.id }
        }
        .first()
    }
  }

  fun isModified(): Boolean {
    return saved != null && _providers.value != saved
  }

  fun getChangedProviders(): List<LlmProviderInstance> {
    val currentProviders = _providers.value
    return if (saved != null) {
      currentProviders.filter { provider ->
        val savedProvider = saved?.get(provider.id)
        savedProvider == null || savedProvider.settings != provider.settings
      }
    }
    else {
      currentProviders
    }
  }

  var settingsUpdaterJob: Job? = null

  fun updateSelectedProvider(selectedValue: LlmProviderInstance?) {
    val currentProvider = _selectedProvider.value
    _selectedProvider.value = selectedValue
    _providerConfigurable.update { current ->
      if (current != null) {
        _providers.update { providers ->
          providers.map {
            if (it.id == currentProvider?.id) {
              it.copy(settings = current.getSettings().value)
            }
            else {
              it
            }
          }
        }
        current.dispose()
      }
      settingsUpdaterJob?.cancel()
      settingsUpdaterJob = null
      selectedValue?.let {
        val configurable = createLlmProviderSettingsConfigurable(it)
        settingsUpdaterJob = coroutineScope.launch {
          configurable.getSettings().collect { settings ->
            _providers.update { instances ->
              instances.map { instance ->
                if (instance.id == selectedValue.id) {
                  selectedValue.copy(
                    settings = settings
                  )
                }
                else {
                  instance
                }
              }
            }
          }
        }
        configurable
      }
    }
  }

  fun removeProvider(provider: LlmProviderInstance) {
    _providers.update { it.filter { it.id != provider.id } }
    coroutineScope.launch {
      LlmServiceManager.getInstance(project).removeProviderApiKeyIfNoProviders(provider.provider)
    }
  }

  fun getNotSelectedProviders(): Flow<List<LlmProvider>> {
    return combine(
      LlmServiceManager.getInstance(project).availableProvidersState,
      _providers
    ) { supported, providers ->
      val existingProviders = providers.map { it.provider }.toSet()
      supported.filter { it.canHaveMultipleInstances || !existingProviders.contains(it) }
    }
  }

  fun addProvider(selectedValue: LlmProvider) {
    val llmProviderInstance = LlmProviderInstance(
      provider = selectedValue,
      settings = LlmServiceManager.getInstance(project).createProviderSettings(selectedValue)
    )
    _providers.update {
      it + llmProviderInstance
    }
    coroutineScope.launch {
      LlmServiceManager.getInstance(project).removeProviderApiKeyIfNoProviders(selectedValue)
    }
    updateSelectedProvider(llmProviderInstance)
  }

  fun createLlmProviderSettingsConfigurable(instance: LlmProviderInstance): LlmProviderSettingsConfigurable {
    return LlmSettingsManager.getInstance(project).createProviderSettingsConfigurable(instance.provider, instance.settings)
  }

  fun updateShowAPIKeysFoundBanner(value: Boolean) {
    _showAPIKeysFoundBanner.value = value
  }

  fun dispose() {
    coroutineScope.cancel()
    _providerConfigurable.value?.dispose()
  }

}
