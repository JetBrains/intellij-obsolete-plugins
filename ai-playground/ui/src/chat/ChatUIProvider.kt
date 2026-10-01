package com.intellij.aiplayground.ui.chat

import com.intellij.aiplayground.models.chat.Chat

interface ChatUiProvider {

  suspend fun openChat(chat: Chat): ChatViewModel?

}