package com.intellij.aiplayground.models.chat

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Service for storing chats per project using IntelliJ's persistence mechanism.
 */
@Service(Service.Level.PROJECT)
@State(name = "AIPlaygroundChatStorage", storages = [Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)])
class ChatStorageService(val project: Project, val coroutineScope: CoroutineScope) : 
    PersistentStateComponent<ChatStorageService.State> {
    
    // All available chats
    private val _allChats = MutableStateFlow<Map<ChatId, Chat>>(emptyMap())
    
    private val _events = MutableSharedFlow<ChatEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    
    /**
     * Get all available chats
     */
    fun getAllChats(): Flow<List<Chat>> = _allChats.map { it.values.sortedByDescending { it.updatedAt } }
    
    /**
     * Get a specific chat by ID
     */
    fun getChat(id: ChatId): Chat? {
        return _allChats.value[id]
    }
    
    /**
     * Get a flow of a specific chat by ID
     */
    fun getChatFlow(chatId: ChatId): Flow<Chat> {
        return _allChats.transformWhile { map ->
            map[chatId]?.let {
                emit(it)
                true
            } ?: false
        }
    }
    
    /**
     * Get a flow of chat events
     */
    fun getEventsFlow(): Flow<ChatEvent> = _events
    
    /**
     * Save a chat
     */
    fun saveChat(chat: Chat) {
        _allChats.update { currentChats ->
            currentChats.toMutableMap().apply {
                this[chat.id] = chat
            }
        }
        coroutineScope.launch {
            _events.emit(ChatEvent.ChatUpdated(chat))
        }
    }
    
    /**
     * Update a chat
     */
    fun updateChat(id: ChatId, updater: (Chat) -> Chat): Chat {
        val chats = _allChats.updateAndGet { currentChats ->
            val updated = currentChats[id]?.let { updater(it) }?.copy(updatedAt = System.currentTimeMillis())
                          ?: error("Chat with ID $id not found")
            currentChats.toMutableMap().apply {
                put(id, updated)
            }
        }
        val chat = chats[id] ?: error("Chat with ID $id not found")
        coroutineScope.launch {
            _events.emit(ChatEvent.ChatUpdated(chat))
        }
        return chat
    }
    
    /**
     * Delete a chat
     */
    fun deleteChat(id: ChatId) {
        val chat = getChat(id)
        // Remove from chat list
        _allChats.update { currentChats ->
            currentChats.toMutableMap().apply { remove(id) }
        }
        
        chat?.let {
            coroutineScope.launch {
                _events.emit(ChatEvent.ChatRemoved(it))
            }
        }
    }
    
    /**
     * Create a new chat
     */
    fun createChat(title: String? = null): Chat {
        val initialModels = service<PlaygroundSettings>().getRecentModels().firstOrNull()?.let { (llmProviderInstanceId, llmModelId) ->
            project.service<LlmServiceManager>().configuredProviders.firstOrNull { it.id == llmProviderInstanceId }?.let {
              providerInstance -> ChatModelLink(providerId = providerInstance.provider.id, instanceId = providerInstance.id, modelId = llmModelId)
            }
        }?.let { listOf(it) } ?: emptyList()
        val newChat = Chat(title = title, activeModels = initialModels)

        _allChats.update { currentChats ->
            currentChats.toMutableMap().apply {
                this[newChat.id] = newChat
            }
        }
        
        coroutineScope.launch {
            _events.emit(ChatEvent.ChatCreated(newChat))
        }
        
        return newChat
    }
    
    /**
     * State class for persisting chats
     */
    class State {
        var chats: List<SerializedChat> = mutableListOf()
    }
    
    /**
     * Serialized chat for persistence
     */
    class SerializedChat {
        var id: String = ""
        var title: String? = null
        var messages: List<SerializedChatMessage> = mutableListOf()
        var createdAt: Long = 0
        var updatedAt: Long = 0
        var activeModels: List<SerializedChatModelLink> = mutableListOf()
        
        fun toChat(): Chat {
            return Chat(
                id = ChatId(id),
                title = title,
                messages = messages.mapNotNull { it.toChatMessage() },
                createdAt = createdAt,
                updatedAt = updatedAt,
                activeModels = activeModels.map { it.toChatModelLink() }
            )
        }
        
        companion object {
            fun fromChat(chat: Chat): SerializedChat {
                return SerializedChat().apply {
                    id = chat.id.id
                    title = chat.title
                    messages = chat.messages.map { SerializedChatMessage.fromChatMessage(it) }
                    createdAt = chat.createdAt
                    updatedAt = chat.updatedAt
                    activeModels = chat.activeModels.map { SerializedChatModelLink.fromChatModelLink(it) }
                }
            }
        }
    }
    
    /**
     * Serialized chat message for persistence
     */
    class SerializedChatMessage {
        var type: String = ""
        var id: String = ""
        var content: String = ""
        var modelId: String? = null
        var isStreaming: Boolean = false
        var isError: Boolean = false
        var timestamp: Long = 0
        var tokenUsage: SerializedTokenUsage? = null
        
        fun toChatMessage(): ChatMessage? {
            val messageId = ChatMessageId(id)
            return when (type) {
                "user" -> UserMessage(messageId, content, timestamp)
                "assistant" -> {
                    val modelLink = modelId?.let { ChatModelLinkId(it) }?.let { id ->
                        // Find the model link in the active models
                        // This is a simplification - in a real implementation, you would need to
                        // store more information about the model link
                        ChatModelLink(
                            id = id,
                            providerId = LlmProviderId("unknown"),
                            instanceId = LlmProviderInstanceId("unknown"),
                            modelId = LlmModelId("unknown"),
                            parameters = ChatRequestConfig()
                        )
                    }
                    modelLink?.let {
                        AssistantMessage(
                            id = messageId,
                            content = content,
                            model = it,
                            isStreaming = isStreaming,
                            isError = isError,
                            timestamp = timestamp,
                            tokenUsage = tokenUsage?.toTokenUsage()
                        )
                    }
                }
                "system" -> SystemMessage(messageId, content, timestamp)
                else -> null
            }
        }
        
        companion object {
            fun fromChatMessage(message: ChatMessage): SerializedChatMessage {
                return SerializedChatMessage().apply {
                    id = message.id.id
                    timestamp = message.timestamp
                    
                    when (message) {
                        is UserMessage -> {
                            type = "user"
                            content = message.content
                        }
                        is AssistantMessage -> {
                            type = "assistant"
                            content = message.content
                            modelId = message.model.id.id
                            isStreaming = message.isStreaming
                            isError = message.isError
                            tokenUsage = message.tokenUsage?.let { SerializedTokenUsage.fromTokenUsage(it) }
                        }
                        is SystemMessage -> {
                            type = "system"
                            content = message.content
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Serialized token usage for persistence
     */
    class SerializedTokenUsage {
        var inputTokenCount: Int = 0
        var outputTokenCount: Int = 0
        var totalTokenCount: Int = 0
        
        fun toTokenUsage(): TokenUsage {
            return TokenUsage(
                inputTokenCount = inputTokenCount,
                outputTokenCount = outputTokenCount,
                totalTokenCount = totalTokenCount
            )
        }
        
        companion object {
            fun fromTokenUsage(tokenUsage: TokenUsage): SerializedTokenUsage {
                return SerializedTokenUsage().apply {
                    inputTokenCount = tokenUsage.inputTokenCount
                    outputTokenCount = tokenUsage.outputTokenCount
                    totalTokenCount = tokenUsage.totalTokenCount
                }
            }
        }
    }
    
    /**
     * Serialized chat model link for persistence
     */
    class SerializedChatModelLink {
        var id: String = ""
        var providerId: String = ""
        var instanceId: String = ""
        var modelId: String = ""
        
        fun toChatModelLink(): ChatModelLink {
            return ChatModelLink(
                id = ChatModelLinkId(id),
                providerId = LlmProviderId(providerId),
                instanceId = LlmProviderInstanceId(instanceId),
                modelId = LlmModelId(modelId),
                parameters = ChatRequestConfig()
            )
        }
        
        companion object {
            fun fromChatModelLink(link: ChatModelLink): SerializedChatModelLink {
                return SerializedChatModelLink().apply {
                    id = link.id.id
                    providerId = link.providerId.id
                    instanceId = link.instanceId.id
                    modelId = link.modelId.id
                }
            }
        }
    }
    
    override fun getState(): State {
        val state = State()
        state.chats = _allChats.value.values.map { SerializedChat.fromChat(it) }
        return state
    }
    
    override fun loadState(state: State) {
        val chats = state.chats.mapNotNull { it.toChat() }
        _allChats.value = chats.associateBy { it.id }
    }
}