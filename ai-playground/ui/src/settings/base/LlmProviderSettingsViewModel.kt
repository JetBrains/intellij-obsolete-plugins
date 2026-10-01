package com.intellij.aiplayground.ui.settings.base

import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class LlmProviderSettingsViewModel<S : LlmProviderSettings<S>>(
  private val project: Project,
  parentScope: CoroutineScope,
  protected val provider: LlmProvider,
  providerSettings: S,
) {

  private val coroutineScope = parentScope.childScope("LlmProviderSettingsViewModel")

  protected val _settings: MutableStateFlow<S> = MutableStateFlow(providerSettings)
  val settings: StateFlow<S> = _settings.asStateFlow()

  private val _connected = MutableStateFlow<ConnectionState>(ConnectionState.UNKNOWN)
  val connected: StateFlow<ConnectionState> = _connected.asStateFlow()

  private val _disabledModels = MutableStateFlow<Set<String>>(emptySet())
  val disabledModels: StateFlow<Set<String>> = _disabledModels.asStateFlow()

  private val _availableModels = MutableStateFlow<List<LlmModel>>(emptyList())
  val availableModels: StateFlow<List<LlmModel>> = _availableModels.asStateFlow()

  init {
    // Load disabled models from settings for this provider
    _disabledModels.value = service<PlaygroundSettings>().getDisabledModels(provider.id).map { it.id }.toSet()

    // Refresh available models whenever settings change
    coroutineScope.launch {
      settings
        .flatMapLatest { currentSettings ->
          val instance = LlmProviderInstance(provider = provider, settings = currentSettings)
          LlmServiceManager.getInstance(project).getSupportedModels(instance)
        }
        .collectLatest { models ->
          _availableModels.value = models
        }
    }
  }

  fun testConnection() {
    _connected.value = ConnectionState.CONNECTING
    coroutineScope.launch {
      try {
        withContext(Dispatchers.IO) {
          LlmServiceManager.getInstance(project).testConnection(provider, settings.value)
        }
        _connected.value = ConnectionState.CONNECTED
      }
      catch (_: Exception) {
        _connected.value = ConnectionState.FAILED
      }
    }
  }

  fun updateName(name: String) {
    _settings.update {
      it.updateName(name)
    }
  }

  fun updateEnabled(enabled: Boolean) {
    _settings.update {
      it.updateEnabled(enabled)
    }
  }

  fun setDisabledModels(ids: Set<String>) {
    _disabledModels.value = ids
    service<PlaygroundSettings>().setDisabledModels(provider.id, ids.map { LlmModelId(it) }.toSet())
  }

  fun disableModels(ids: Collection<String>) {
    if (ids.isEmpty()) return
    setDisabledModels(_disabledModels.value + ids)
  }

  fun enableModels(ids: Collection<String>) {
    if (ids.isEmpty()) return
    setDisabledModels(_disabledModels.value - ids.toSet())
  }

  fun dispose() {
    coroutineScope.cancel()
  }

  enum class ConnectionState {
    UNKNOWN,
    CONNECTING,
    CONNECTED,
    FAILED
  }

}