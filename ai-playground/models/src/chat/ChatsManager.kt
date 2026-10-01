package com.intellij.aiplayground.models.chat

import com.intellij.aiplayground.models.ChatRequestConfig
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.RequestContext
import com.intellij.aiplayground.models.statistic.MessageType
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ProgressTitle
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.Nls
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class ChatsManager(private val project: Project, val coroutineScope: CoroutineScope) {

  // Mutex for synchronizing chat history updates
  private val chatHistoryMutex = Mutex()

  private val chatScopes = mutableMapOf<ChatId, CoroutineScope>()

  fun sendMessage(
    chatId: ChatId,
    userMessage: String,
    specificProviders: Set<ChatModelLink>?,
    jobTitle: @ProgressTitle String,
  ): Unit = startChatStreaming(chatId) {
    // Log the start of message sending
    logger.debug("Starting to send message '${userMessage.take(20)}...' to ${specificProviders?.size ?: "all"} providers")
    coroutineScope {
      val chat = addMessageToChat(chatId) {
        UserMessage(it, userMessage)
      }
      logger.debug("Added user message to chat history, now ${chat.messages.size} messages")

      // Update the chat title based on first user message
      updateChatTitleFromContent(chatId)
    }

    completeChat(chatId, specificProviders, jobTitle)
  }

  fun startChatStreaming(chatId: ChatId, block: suspend () -> Unit) {
    chatScopes.getOrPut(chatId) { coroutineScope.childScope("Chat ${chatId.id}") }.launch {
      project.service<ChatRepository>().updateChat(chatId) {
        it.copy(isStreaming = true)
      }
      try {
        block()
      }
      catch (e: CancellationException) {
        logger.debug("Streaming cancelled for chat ${chatId.id}")
        throw e
      }
      finally {
        project.service<ChatRepository>().updateChat(chatId) {
          it.copy(isStreaming = false)
        }
        updateChatHistory(chatId) {
          it.map { message -> if (message is AssistantMessage && message.isStreaming) message.copy(isStreaming = false) else message }
        }
      }
    }
  }

  private suspend fun completeChat(
    chatId: ChatId,
    specificProviders: Set<ChatModelLink>? = null,
    jobTitle: @ProgressTitle @Nls String,
  ) {
    @Suppress("HardCodedStringLiteral")
    withBackgroundProgress(project, jobTitle, cancellable = true) {
      val chat = project.service<ChatRepository>().getChat(chatId) ?: error("Chat $chatId not found")
      // Use specified providers if provided, otherwise use all active models
      val modelsToUse = if (specificProviders != null && specificProviders.isNotEmpty()) {
        specificProviders
      }
      else {
        chat.activeModels.filter { it.show }.toSet()
      }

      logger.debug("Will use ${modelsToUse.size} models for response")

      if (modelsToUse.isEmpty()) {
        logger.debug("No models to use")
      }
      else {
        coroutineScope {
          modelsToUse.map { link ->
            async {
              logger.debug("Sending message to provider ${link.providerId} with model ${link.modelId}")
              try {
                // Create a placeholder for assistant response that will be updated
                val assistantMessageId = ChatMessageId()

                val chat = addMessageToChat(chatId) {
                  AssistantMessage(id = assistantMessageId, content = "", model = link, isStreaming = true)
                }
                logger.debug("Added placeholder for ${link.providerId}/${link.modelId}, now ${chat.messages.size} messages")

                completeAssistantMessage(chatId, assistantMessageId, link.parameters)
              }
              catch (e: CancellationException) {
                throw e
              }
              catch (e: Exception) {
                // Catch any exceptions outside the API call itself
                logger.error("Unexpected error sending to ${link.providerId}: ${e.message}")
              }
            }
          }.awaitAll()
        }
      }
    }
  }

  private suspend fun completeAssistantMessage(
    chatId: ChatId,
    messageId: ChatMessageId,
    requestConfig: ChatRequestConfig,
  ) {
    val chat = project.service<ChatRepository>().getChat(chatId) ?: error("Chat $chatId not found")
    val message = chat.messages.find { it.id == messageId } as? AssistantMessage ?: error("Message $messageId not found")
    try {
      val instanceId = message.model.instanceId
      val modelId = message.model.modelId
      val messages = chat.messages
        .asSequence()
        .takeWhile { it.id != messageId }
        .filter { (it is AssistantMessage && it.model.instanceId == instanceId && it.model.modelId == modelId) || it is UserMessage }
        .toList()

      val service = project.service<LlmServiceManager>()
      val instance = project.service<LlmServiceManager>().configuredProviders.firstOrNull { it.id == message.model.instanceId }
                     ?: error("Provider instance ${message.model.instanceId} not found")
      val model = service.getModel(instance, message.model.modelId)
      service.streamingComplete(
        model = model,
        prompt = "",
        context = RequestContext(chat.systemPrompt, messages),
        requestConfig = requestConfig,
        instance = instance
      ).collect { event ->
        when (event) {
          is ChatResponseEvent.PartialContentResponseEvent -> {
            updateChatHistory(chat.id) {
              it.map { message ->
                if (message is AssistantMessage && message.id == messageId) {
                  message.copy(content = message.content + event.partialContent)
                }
                else {
                  message
                }
              }
            }
          }
          is ChatResponseEvent.TokenUsageResponseEvent -> {
            updateChatHistory(chat.id) {
              it.map { message ->
                if (message is AssistantMessage && message.id == messageId) {
                  message.copy(tokenUsage = event.tokenUsage)
                }
                else {
                  message
                }
              }
            }
          }
        }
      }

      val finalizedChat = updateChatHistory(chat.id) {
        it.map { message ->
          if (message is AssistantMessage && message.id == messageId) {
            message.copy(isStreaming = false)
          }
          else {
            message
          }
        }
      }

      val fm = finalizedChat.messages.find { it.id == messageId } as? AssistantMessage
      fm?.let {
        PlaygroundCollector.logMessageCompleted(model.provider.id.id, model.id.id, fm.tokenUsage?.totalTokenCount ?: 0)
      }

      logger.debug("Finalized message from ${instanceId.id}/${message.model.id}, now ${finalizedChat.messages.size} messages")
    }
    catch (e: Exception) {
      rethrowControlFlowException(e)

      val errorChat = updateChatHistory(chat.id) {
        it.map { message ->
          if (message is AssistantMessage && message.id == messageId) {
            message.copy(
              content = "Error: ${e.message}",
              isError = true,
              isStreaming = false
            )
          }
          else {
            message
          }
        }
      }
      logger.error("Error message from ${message.model.instanceId}/${message.model.id}, now ${errorChat.messages.size} messages", e)
    }
  }

  /**
   * Regenerates the last assistant response
   */
  fun regenerateResponse(chatId: ChatId, messageId: ChatMessageId): Unit = startChatStreaming(chatId) {
    val chat = project.service<ChatRepository>().getChat(chatId) ?: return@startChatStreaming
    val message = chat.messages.findLast { it.id == messageId } as? AssistantMessage ?: return@startChatStreaming
    val requestConfig = chat.activeModels.find { it.id == message.model.id }!!.parameters

    updateChatHistory(chatId) {
      data class Context(val messages: MutableList<ChatMessage> = mutableListOf(), val chatModelLinkId: ChatModelLinkId? = null)
      val (messages, _) = it.fold(Context()) { acc, message ->
        when (message) {
          is AssistantMessage if message.id == messageId -> {
            acc.messages.add(AssistantMessage(id = message.id, content = "", model = message.model, isStreaming = true))
            Context(acc.messages, message.model.id)
          }
          is AssistantMessage if message.model.id == acc.chatModelLinkId -> {
            acc.messages.add(AssistantMessage(id = message.id, content = "", model = message.model, isStreaming = true))
            acc
          }
          else -> {
            acc.messages.add(message)
            acc
          }
        }
      }
      messages
    }
    completeAssistantMessage(chatId, messageId, requestConfig)
    PlaygroundCollector.logMessageRegenerated()
  }

  private suspend fun addMessageToChat(
    chatId: ChatId,
    messageCreator: (ChatMessageId) -> ChatMessage,
  ): Chat {
    val messageId = ChatMessageId()
    return updateChatHistory(chatId) {
      it + messageCreator(messageId)
    }
  }

  private suspend fun updateChatHistory(
    chatId: ChatId,
    historyUpdater: (List<ChatMessage>) -> List<ChatMessage>,
  ): Chat {
    return chatHistoryMutex.withLock {
      project.service<ChatRepository>().updateChat(chatId) {
        it.copy(messages = historyUpdater(it.messages))
      }
    }
  }

  /**
   * Update the chat title based on its content if not already set
   */
  fun updateChatTitleFromContent(chatId: ChatId): Chat {
    return project.service<ChatRepository>().updateChat(chatId) {
      if (it.title == null) {
        val generatedTitle = it.generateSummaryTitle()
        if (generatedTitle != null) {
          it.copy(title = generatedTitle)
        }
        else {
          it
        }
      }
      else {
        it
      }
    }
  }

  suspend fun updateMessage(chatId: ChatId, messageId: ChatMessageId, text: String, jobTitle: @ProgressTitle String): Chat {
    var message: ChatMessage? = null
    val chat = updateChatHistory(chatId) { messages ->
      val updatedMessages = mutableListOf<ChatMessage>()
      for (m in messages) {
        updatedMessages.add(if (m.id == messageId) {
          message = m
          m.updateText(text)
        }
                            else {
          m
        })
        if (message is UserMessage) break
      }
      updatedMessages
    }
    if (message != null) {
      PlaygroundCollector.logMessageEdited(
        when (message) {
          is UserMessage -> MessageType.USER
          is AssistantMessage -> MessageType.ASSISTANT
          is SystemMessage -> MessageType.SYSTEM
        }
      )
      if (message is UserMessage) {
        completeChat(chatId, null, jobTitle)
      }
    }
    return chat
  }

  fun stopStreaming(chatId: ChatId) {
    chatScopes.remove(chatId)?.cancel()
  }

  companion object {
    private val logger by lazy { Logger.getInstance(ChatsManager::class.java) }

  }
}