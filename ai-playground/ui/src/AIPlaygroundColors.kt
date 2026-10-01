package com.intellij.aiplayground.ui

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor

object AIPlaygroundColors {
  /**
   * From AI Assistant
   *
   * See [com.intellij.ml.llm.core.chat.ui.AIAssistantColors.USER_MESSAGE_BACKGROUND]
   * */
  val USER_MESSAGE_BACKGROUND_COLOR: JBColor = JBColor.namedColor("AIPlayground.Chat.UserMessage.background", JBColor(0xE8E9EB, 0x3A3C3E))
  val ASSISTANT_MESSAGE_BACKGROUND_COLOR: JBColor = JBColor.namedColor("AIPlayground.Chat.AssistantMessage.background", JBColor(0xF5F5F5, 0x2B2B2B))
  val TOOL_WINDOW_ICON_COLOR: JBColor = JBColor.namedColor("AIPlayground.ToolWindow.iconColor", JBColor(0x3574F0, 0x548AF7))

  /**
   * From [com.intellij.ml.llm.codeGeneration.multifile.ui.ChangeInEditorDiffRendererKt.AI_INLAY_TEXT_COLOR]
   */
  val AI_INLAY_HINT_HIGHLIGHT_PROMPT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("INLINE_PROMPT", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
  val AI_INLAY_HINT_HIGHLIGHT_PROMPT_PLACEHOLDER_BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey("AIPlayground.Inlay.HighlightPromptPlaceholderBraces", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
  val AI_INLAY_HINT_HIGHLIGHT_PROMPT_PLACEHOLDER_TEXT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("AIPlayground.Inlay.HighlightPromptPlaceholderText", DefaultLanguageHighlighterColors.PARAMETER)
}

