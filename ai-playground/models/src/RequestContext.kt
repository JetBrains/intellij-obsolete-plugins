package com.intellij.aiplayground.models

import com.intellij.aiplayground.models.chat.ChatMessage

class RequestContext(val systemPrompt: String, val messages: List<ChatMessage>) {
}