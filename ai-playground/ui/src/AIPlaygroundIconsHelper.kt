package com.intellij.aiplayground.ui

import javax.swing.Icon
import com.intellij.aiplayground.ui.icons.AiplaygroundUIIcons

object AIPlaygroundIconsHelper {
  fun providerName2Icon(providerId: String): Icon {
    return when (providerId.lowercase()) {
      "openai", "openai_compatible" -> AiplaygroundUIIcons.OpenAI
      "anthropic" -> AiplaygroundUIIcons.Anthropic
      "mistral" -> AiplaygroundUIIcons.Mistral
      "ollama" -> AiplaygroundUIIcons.Ollama
      "deepseek" -> AiplaygroundUIIcons.DeepSeek
      "gemini", "google" -> AiplaygroundUIIcons.Gemini
      "open_router", "openrouter" -> AiplaygroundUIIcons.OpenRouter
      "aiassistant" -> AiplaygroundUIIcons.Aiassistant
      else -> AiplaygroundUIIcons.ToolWindowAIPlayground
    }
  }
}