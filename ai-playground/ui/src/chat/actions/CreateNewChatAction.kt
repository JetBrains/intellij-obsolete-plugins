package com.intellij.aiplayground.ui.chat.actions

import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.statistic.CreateChatPlace
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.launch
import javax.swing.JComponent

class CreateNewChatAction() : AnAction(AIPlaygroundUIBundle.message("action.new.playground.text"), AIPlaygroundUIBundle.message("action.new.playground.description"), AllIcons.General.Add), CustomComponentAction, DumbAware {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val newChat = project.service<ChatRepository>().createChat()
    PlaygroundCollector.logChatCreated(CreateChatPlace.TOOL_WINDOW_ACTION)
    service<AiPlaygroundCoroutine>().coroutineScope.launch {
      project.service<ChatUiProvider>().openChat(newChat)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    val button = ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE).apply {
      accessibleContext.accessibleName = presentation.text ?: presentation.description
    }
    return Wrapper(button).apply {
      border = JBUI.Borders.empty(0, 2)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null
  }
}