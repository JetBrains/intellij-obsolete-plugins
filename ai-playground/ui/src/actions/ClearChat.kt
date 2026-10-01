package com.intellij.aiplayground.ui.actions

import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction


class ClearChat : DumbAwareAction(AIPlaygroundUIBundle.message("action.clear.chat.history.text"), null, AllIcons.Actions.ClearCash) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    e.dataContext.getData(CHAT)?.let { chat ->
      project.service<ChatRepository>().updateChat(chat.id) {
        it.copy(messages = emptyList())
      }
      PlaygroundCollector.logChatHistoryCleared()
    }
  }
}