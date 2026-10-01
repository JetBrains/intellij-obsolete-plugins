package com.intellij.aiplayground.models.chat

import com.intellij.aiplayground.models.LlmModel

/**
 * Simple provider-agnostic message representation used for building request payloads
 */
data class GenericMessage(val role: String, val content: String)

/**
 * Build a normalized list of role/content messages based on the prompt, optional system prompt and prior conversation.
 * The algorithm ensures:
 * - Optional system message goes first if provided and not blank
 * - If no prior messages, uses the prompt as the first user message
 * - When multiple consecutive assistant messages appear, pick the one matching the current model if present, otherwise the first
 * - The resulting list always ends with a user message (drops trailing assistant message)
 */
fun buildGenericMessages(
  model: LlmModel,
  prompt: String,
  systemPrompt: String?,
  messages: List<ChatMessage>?,
): List<GenericMessage> = buildList {
  // System
  systemPrompt?.takeIf { it.isNotBlank() }?.let { add(GenericMessage("system", it)) }

  if (messages.isNullOrEmpty()) {
    add(GenericMessage("user", prompt))
    return@buildList
  }

  var lastRole: String? = null
  var index = 0
  while (index < messages.size) {
    when (val message = messages[index]) {
      is UserMessage -> {
        add(GenericMessage("user", message.content))
        lastRole = "user"
        index++
      }
      is AssistantMessage -> {
        if (lastRole == "assistant") { index++; continue }
        val consecutive = messages.subList(index, messages.size)
          .takeWhile { it is AssistantMessage }
          .filterIsInstance<AssistantMessage>()
        consecutive.find { it.model.id.id == model.id.id }
          ?.let { selected ->
            add(GenericMessage("assistant", selected.content))
            lastRole = "assistant"
          } ?: consecutive.firstOrNull()?.let { first ->
            add(GenericMessage("assistant", first.content))
            lastRole = "assistant"
          }
        index += consecutive.size
      }
      else -> index++
    }
  }

  if (isNotEmpty() && last().role == "assistant") {
    removeLast()
  }
}
