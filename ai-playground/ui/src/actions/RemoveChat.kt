package com.intellij.aiplayground.ui.actions

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import javax.swing.Action
import javax.swing.JComponent


class RemoveChat : DumbAwareAction(AIPlaygroundUIBundle.message("action.remove.chat.text"), null, AllIcons.General.Remove) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    e.dataContext.getData(CHAT)?.let { chat ->
      project.service<ChatRepository>().deleteChat(chat.id)
      PlaygroundCollector.logChatRemoved()
    }
  }
}

class RemoveChatDialog(
  val project: Project,
  val chats: List<Chat>,
) : DialogWrapper(project) {
  init {
    title = AIPlaygroundUIBundle.message("action.remove.many.chats.title")
    init()
    okAction.putValue(Action.NAME, AIPlaygroundUIBundle.message("action.remove.many.chats.ok"))
    cancelAction.putValue(Action.NAME, AIPlaygroundUIBundle.message("action.remove.many.chats.cancel"))
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label(AIPlaygroundUIBundle.message("action.remove.many.chats.text", chats.size))
      }
    }
  }
}