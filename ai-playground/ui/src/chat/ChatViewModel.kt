package com.intellij.aiplayground.ui.chat

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatId
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatMessageId
import com.intellij.aiplayground.models.chat.ChatModelLink
import com.intellij.aiplayground.models.chat.ChatModelLinkId
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.chat.ChatsManager
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
  val project: Project,
  private val parentScope: CoroutineScope,
  chat: Chat,
) {
  private val coroutineScope = parentScope.childScope("ChatViewModel")

  private val chatId: ChatId = chat.id

  val chatHistory: StateFlow<List<ChatMessage>> = project.service<ChatRepository>().getChatFlow(chatId).map { chat ->
    chat.messages
  }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

  val activeChat: StateFlow<Chat> = project.service<ChatRepository>().getChatFlow(chatId)
    .stateIn(coroutineScope, SharingStarted.Eagerly, chat)

  val isStreaming: StateFlow<Boolean> = activeChat.map { chat -> chat.isStreaming }
    .stateIn(coroutineScope, SharingStarted.Eagerly, false)

  // Selected provider state
  private val _selectedModel = MutableStateFlow<ChatModelLink?>(null)
  val selectedModel: StateFlow<ChatModelLink?> = _selectedModel.asStateFlow()

  // Accordion expand state — at most one model link can be expanded at a time
  private val _expandedModelLinkId = MutableStateFlow<ChatModelLinkId?>(null)
  val expandedModelLinkId: StateFlow<ChatModelLinkId?> = _expandedModelLinkId.asStateFlow()

  val activeModels: StateFlow<List<ChatModelLink>> = activeChat.map { chat -> chat.activeModels }
    .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

  // Cache of ChatModelLink ViewModels, lazily populated via getModelLinkViewModel
  private val modelLinkViewModels = mutableMapOf<ChatModelLinkId, ChatModelLinkViewModel>()

  init {
    // Cleanup: dispose ViewModels whose IDs are no longer in activeModels
    coroutineScope.launch {
      activeModels.collectLatest { models ->
        val activeIds = models.map { it.id }.toSet()
        val removedIds = modelLinkViewModels.keys - activeIds
        removedIds.forEach { id ->
          modelLinkViewModels.remove(id)?.dispose()
        }
      }
    }
  }

  fun getModelLinkViewModel(id: ChatModelLinkId): ChatModelLinkViewModel {
    return modelLinkViewModels.getOrPut(id) {
      ChatModelLinkViewModel(project, coroutineScope, id, chatId, activeModels, ::toggleExpandedModelLink, ::removeActiveModel)
    }
  }

  val systemPromptText: StateFlow<String> = activeChat.map { chat -> chat.systemPrompt }
    .stateIn(coroutineScope, SharingStarted.Eagerly, "")

  // Playground settings state
  private val _editingMessage = MutableStateFlow<ChatMessageId?>(null)
  val editingMessage: StateFlow<ChatMessageId?> = _editingMessage.asStateFlow()

  private val chatRepository: ChatRepository
    get() = ChatRepository.getInstance(project)

  private val _input = MutableStateFlow("")
  val input: StateFlow<String> = _input.asStateFlow()

  fun selectModelForProvider(link: ChatModelLink) {
    _selectedModel.value = link
  }

  /**
   * Toggles the expanded state of a model link in the accordion.
   * Collapses the previously expanded link (if any) and expands the clicked one,
   * or collapses it if it was already expanded.
   */
  fun toggleExpandedModelLink(clickedId: ChatModelLinkId) {
    val prev = _expandedModelLinkId.value
    val next = if (prev == clickedId) null else clickedId
    _expandedModelLinkId.value = next
    if (next != null) {
      activeModels.value.find { it.id == next }?.let { selectModelForProvider(it) }
    }
  }

  /**
   * Sends a user message and gets responses from all active models
   * If no models are active, it will still show the user message but prompt to add a model
   * @param specificProviders Optional set of specific providers to use (for multi-provider UI)
   */
  fun sendMessage(userMessage: String, specificProviders: Set<ChatModelLink>?) {
    if (userMessage.isBlank()) return

    project.service<ChatsManager>()
      .sendMessage(chatId, userMessage, specificProviders, AIPlaygroundUIBundle.message("progress.title.exchange"))
    val chat = activeChat.value
    PlaygroundCollector.logPromptSubmitted(chat.activeModels.size, chat.systemPrompt.isNotBlank())
  }

  /**
   * Original sendMessage method for backward compatibility
   */
  fun sendMessage(userMessage: String) {
    sendMessage(userMessage, null)
  }

  fun regenerateLastResponse(message: AssistantMessage) {
    coroutineScope.launch {
      project.service<ChatsManager>().regenerateResponse(chatId, message.id)
    }
  }

  fun addActiveModel(instance: LlmProviderInstance, modelId: LlmModelId) {
    val newLink = ChatModelLink(providerId = instance.provider.id, modelId = modelId, instanceId = instance.id)
    chatRepository.updateChat(chatId) {
      it.copy(activeModels = it.activeModels + newLink)
    }
    // If no provider is selected, select this one
    if (_selectedModel.value == null) {
      _selectedModel.value = newLink
    }

    // Select this model for the provider
    selectModelForProvider(newLink)

    service<PlaygroundSettings>().updateRecentModel(instance.id, modelId)

    PlaygroundCollector.logModelAdded(instance.provider.id.id, modelId.id)
  }

  /**
   * Removes a model from the active models list
   * Note: Will remove even the last model, leaving an empty chat
   */
  fun removeActiveModel(linkId: ChatModelLinkId) {
    val link = activeChat.value.activeModels.find { it.id == linkId } ?: return
    PlaygroundCollector.logModelRemoved(link.providerId.id, link.modelId.id)
    chatRepository.updateChat(chatId) {
      it.copy(activeModels = it.activeModels.filterNot { model -> model.id == linkId })
    }
    if (selectedModel.value == link) {
      _selectedModel.value = null
    }
  }

  fun updateSystemPromptText(text: String) {
    chatRepository.updateChat(chatId) {
      it.copy(systemPrompt = text)
    }
  }

  fun updateInput(text: String) {
    _input.value = text
  }

  fun updateMessage(messageId: ChatMessageId, text: String) {
    coroutineScope.launch {
      _editingMessage.value = null
      project.service<ChatsManager>().updateMessage(chatId, messageId, text, AIPlaygroundUIBundle.message("progress.title.exchange"))
    }
  }

  fun startEditing(id: ChatMessageId) {
    _editingMessage.value = id
  }

  fun cancelEditing(id: ChatMessageId) {
    _editingMessage.compareAndSet(id, null)
  }

  fun stopStreaming() {
    project.service<ChatsManager>().stopStreaming(chatId)
  }

  companion object {
    fun create(project: Project, coroutineScope: CoroutineScope, chat: Chat): ChatViewModel {
      return ChatViewModel(
        project = project,
        parentScope = coroutineScope,
        chat = chat
      )
    }
  }
}
