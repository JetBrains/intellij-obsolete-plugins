package com.intellij.aiplayground.aiassistant

import com.intellij.aiplayground.models.chat.ChatResponseEvent

interface PlaygroundStreamingChatResponseHandler {
  fun onChatResponseEvent(event: ChatResponseEvent)
  fun onError(error: Throwable)
}