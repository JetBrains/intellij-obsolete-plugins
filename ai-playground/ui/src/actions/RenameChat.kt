package com.intellij.aiplayground.ui.actions

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages


val CHAT: DataKey<Chat> = DataKey.create<Chat>("AIPLAYGROUND_CHAT")
val CHAT_LIST: DataKey<List<Chat>> = DataKey.create<List<Chat>>("AIPLAYGROUND_CHAT_LIST")

class RenameChat : DumbAwareAction(AIPlaygroundUIBundle.message("action.rename.chat.text"), null, AllIcons.Actions.Edit) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    e.dataContext.getData(CHAT)?.let { chat ->
      Messages.showInputDialog(project, "", "", null, chat.title ?: "", null)?.let { name ->
        project.service<ChatRepository>().updateChat(chat.id) {
          it.copy(title = name)
        }
        PlaygroundCollector.logChatRenamed()
      }
    }
  }
}