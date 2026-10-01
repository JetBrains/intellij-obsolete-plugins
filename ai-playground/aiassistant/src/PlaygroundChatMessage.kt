package com.intellij.aiplayground.aiassistant

enum class PlaygroundChatRole { SYSTEM, USER, ASSISTANT }

data class PlaygroundChatMessage(val role: PlaygroundChatRole, val content: String)