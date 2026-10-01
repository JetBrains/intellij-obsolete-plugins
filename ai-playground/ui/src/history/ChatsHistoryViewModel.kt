package com.intellij.aiplayground.ui.history

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.CreateChatPlace
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsHistoryViewModel(val project: Project, parentScope: CoroutineScope) {

  private val coroutineScope: CoroutineScope = parentScope.childScope("ChatsHistoryViewModel")

  // Available chats
  private val _availableChats = MutableStateFlow<List<Chat>>(emptyList())
  val availableChats: StateFlow<List<Chat>> = _availableChats.asStateFlow()

  init {
    setupRepositoryCollectors()
  }

  fun newChat() {
    val newChat = project.service<ChatRepository>().createChat()
    PlaygroundCollector.logChatCreated(CreateChatPlace.TOOL_WINDOW_EMPTY_STATE)
    service<AiPlaygroundCoroutine>().coroutineScope.launch {
      project.service<ChatUiProvider>().openChat(newChat)
    }
  }

  private fun setupRepositoryCollectors() {
    coroutineScope.launch {
      // Collect available chats
      project.service<ChatRepository>().getAllChats().collectLatest { chats ->
        _availableChats.value = chats
      }
    }
  }
}