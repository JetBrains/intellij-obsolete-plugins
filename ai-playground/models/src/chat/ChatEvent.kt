package com.intellij.aiplayground.models.chat

sealed interface ChatEvent {

  data class ChatCreated(val chat: Chat) : ChatEvent
  data class ChatUpdated(val chat: Chat) : ChatEvent
  data class ChatRemoved(val chat: Chat) : ChatEvent
}
