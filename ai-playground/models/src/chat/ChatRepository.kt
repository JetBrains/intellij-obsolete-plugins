package com.intellij.aiplayground.models.chat

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing chat operations
 */
interface ChatRepository {
  /**
   * Get all available chats
   */
  fun getAllChats(): Flow<List<Chat>>

  /**
   * Get a specific chat by ID
   */
  fun getChat(id: ChatId): Chat?

  /**
   * Save a chat
   */
  fun saveChat(chat: Chat)

  /**
   * Delete a chat
   */
  fun deleteChat(id: ChatId)

  /**
   * Create a new chat
   */
  fun createChat(title: String? = null): Chat

  fun updateChat(id: ChatId, updater: (Chat) -> Chat): Chat

  fun getChatFlow(chatId: ChatId): Flow<Chat>

  fun getEventsFlow(): Flow<ChatEvent>

  companion object {
    /**
     * Gets the chat repository service instance for the given project
     */
    fun getInstance(project: Project): ChatRepository {
      return project.service<ChatRepository>()
    }
  }
}

/**
 * Service implementation of ChatRepository that persists chats using ChatStorageService
 */
@Service(Service.Level.PROJECT)
class ChatRepositoryService(private val project: Project, val coroutineScope: CoroutineScope) : ChatRepository {
  // Storage service for persisting chats
  private val storageService = project.service<ChatStorageService>()

  override fun getAllChats(): Flow<List<Chat>> = storageService.getAllChats()

  override fun getChat(id: ChatId): Chat? {
    return storageService.getChat(id)
  }

  override fun getChatFlow(chatId: ChatId): Flow<Chat> {
    return storageService.getChatFlow(chatId)
  }

  override fun getEventsFlow(): Flow<ChatEvent> = storageService.getEventsFlow()

  override fun saveChat(chat: Chat) {
    storageService.saveChat(chat)
  }

  override fun updateChat(id: ChatId, updater: (Chat) -> Chat): Chat {
    return storageService.updateChat(id, updater)
  }

  override fun deleteChat(id: ChatId) {
    storageService.deleteChat(id)
  }

  override fun createChat(title: String?): Chat {
    return storageService.createChat(title)
  }
}
