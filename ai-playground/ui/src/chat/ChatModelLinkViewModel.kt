package com.intellij.aiplayground.ui.chat

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.chat.ChatId
import com.intellij.aiplayground.models.chat.ChatModelLink
import com.intellij.aiplayground.models.chat.ChatModelLinkId
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.Parameter
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatModelLinkViewModel(
  private val project: Project,
  parentScope: CoroutineScope,
  val linkId: ChatModelLinkId,
  private val chatId: ChatId,
  activeModelsFlow: StateFlow<List<ChatModelLink>>,
  private val onToggleExpand: (ChatModelLinkId) -> Unit,
  private val onRemove: (ChatModelLinkId) -> Unit,
) {
  private val coroutineScope = parentScope.childScope("ChatModelLinkViewModel-${linkId.id}")

  private val chatRepository: ChatRepository
    get() = ChatRepository.getInstance(project)

  val link: StateFlow<ChatModelLink?> = activeModelsFlow
    .map { models -> models.find { it.id == linkId } }
    .distinctUntilChanged()
    .stateIn(coroutineScope, SharingStarted.Eagerly, activeModelsFlow.value.find { it.id == linkId })

  private val _resolvedInstance = MutableStateFlow<LlmProviderInstance?>(null)
  private val _resolvedModel = MutableStateFlow<LlmModel?>(null)

  val displayName: StateFlow<String> = combine(_resolvedInstance, _resolvedModel, link) { instance, model, currentLink ->
    when {
      instance != null && model != null -> "${instance.settings.displayName} - ${model.displayName}"
      instance != null -> instance.settings.displayName ?: instance.provider.displayName
      else -> currentLink?.modelId?.id ?: linkId.id
    }
  }.stateIn(coroutineScope, SharingStarted.Eagerly, link.value?.modelId?.id ?: linkId.id)

  init {
    coroutineScope.launch {
      val currentLink = link.value ?: return@launch
      val serviceManager = project.service<LlmServiceManager>()

      val instance = serviceManager.configuredProvidersState.value
                       .find { it.id == currentLink.instanceId } ?: return@launch

      _resolvedInstance.value = instance
      _resolvedModel.value = serviceManager.getModel(instance, currentLink.modelId)
    }
  }

  fun toggleExpand() {
    onToggleExpand(linkId)
  }

  fun toggleEnabled() {
    chatRepository.updateChat(chatId) { chat ->
      chat.copy(activeModels = chat.activeModels.map { model ->
        if (model.id == linkId) model.copy(show = !model.show) else model
      })
    }
  }

  fun remove() {
    onRemove(linkId)
  }

  val canApplyConfigToAll: StateFlow<Boolean> = activeModelsFlow
    .map { it.size > 1 }
    .distinctUntilChanged()
    .stateIn(coroutineScope, SharingStarted.Eagerly, activeModelsFlow.value.size > 1)

  fun updateRequestConfig(configUpdater: (ChatRequestConfig) -> ChatRequestConfig) {
    chatRepository.updateChat(chatId) { chat ->
      val result = chat.activeModels.fold(ConfigChangeResult(null, mutableListOf())) { acc, model ->
        when (model.id) {
          linkId -> {
            val updatedConfig = configUpdater(model.parameters)
            val updatedModel = model.copy(parameters = updatedConfig)
            ConfigChangeResult(model.parameters to updatedConfig, acc.updatedModels + updatedModel)
          }
          else -> ConfigChangeResult(acc.changed, acc.updatedModels + model)
        }
      }

      result.changed?.let { (oldConfig, newConfig) ->
        logParameterChanges(oldConfig, newConfig)
      }

      chat.copy(activeModels = result.updatedModels)
    }
  }

  /**
   * Applies this model's request configuration to all other active models.
   */
  fun applyConfigToAll() {
    chatRepository.updateChat(chatId) { chat ->
      val current = chat.activeModels.find { it.id == linkId } ?: return@updateChat chat
      val config = current.parameters
      val updated = chat.activeModels.map { model ->
        if (model.id == linkId) model else model.copy(parameters = config)
      }
      chat.copy(activeModels = updated)
    }
  }

  fun dispose() {
    coroutineScope.cancel()
  }

  private data class ConfigChangeResult(
    val changed: Pair<ChatRequestConfig, ChatRequestConfig>?,
    val updatedModels: List<ChatModelLink>,
  )
}

private fun logParameterChanges(oldConfig: ChatRequestConfig, newConfig: ChatRequestConfig) {
  when {
    oldConfig.topP != newConfig.topP -> PlaygroundCollector.logPromptParametersSet(Parameter.TOP_P)
    oldConfig.maxTokens != newConfig.maxTokens -> PlaygroundCollector.logPromptParametersSet(Parameter.MAX_TOKENS)
    oldConfig.temperature != newConfig.temperature -> PlaygroundCollector.logPromptParametersSet(Parameter.TEMPERATURE)
  }
}
