package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.models.chat.AssistantMessage
import com.intellij.aiplayground.models.chat.ChatMessage
import com.intellij.aiplayground.models.chat.ChatMessageId
import com.intellij.aiplayground.models.chat.ChatModelLink
import com.intellij.aiplayground.models.chat.SystemMessage
import com.intellij.aiplayground.models.chat.UserMessage
import com.intellij.aiplayground.ui.actions.ClearChat
import com.intellij.aiplayground.ui.actions.RemoveChat
import com.intellij.aiplayground.ui.actions.RenameChat
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class MessageHistory(
  private val viewModel: ChatViewModel,
  parentScope: CoroutineScope,
  private val project: Project,
) : JComponent() {

  private val coroutineScope = parentScope.childScope("MessageHistory")
  private val messageComponents = mutableMapOf<ChatMessageId, MessageBubble<ChatMessage>>()
  private var editingMessageComponent: EditableMessageBubble<*>? = null

  private val messagesPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.empty(0, 12)
  }

  init {
    layout = BorderLayout()
    add(messagesPanel, BorderLayout.SOUTH)

    coroutineScope.launch {
      viewModel.activeModels
        .combine(viewModel.chatHistory) { models, message -> models to message }
        .collect { (models, messages) ->
          withContext(Dispatchers.EDT) {
            updateMessages(models, messages)
          }
        }
    }
    coroutineScope.launch {
      viewModel.editingMessage.collect { message ->
        withContext(Dispatchers.EDT) {
          val component = message?.let { messageComponents[it] }
          if (editingMessageComponent != component) {
            editingMessageComponent?.endEdit()
            editingMessageComponent = component as? EditableMessageBubble<*>
            editingMessageComponent?.startEdit()
          }
        }
      }
    }
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e)) {
          val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            DefaultActionGroup(
              listOf(
                ClearChat(),
                RenameChat(),
                RemoveChat(),
              )
            ),
            DataManager.getInstance().getDataContext(this@MessageHistory),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
          )
          popup.show(RelativePoint(e.component, e.point))
        }
      }
    })
  }

  private fun updateMessages(models: List<ChatModelLink>, messages: List<ChatMessage>) {
    val showModels = models.filter { it.show }.map { it.id }.toSet()
    val filtered = messages
      .filterNot { it is SystemMessage }
      .filterNot { it is AssistantMessage && it.model.id !in showModels }

    val currentMessageIds = filtered.map { it.id }.toSet()
    val removedMessageIds = messageComponents.keys - currentMessageIds
    for (id in removedMessageIds) {
      val component = messagesPanel.components.firstOrNull { it is JComponent && it.getClientProperty("messageId") == id }
      messagesPanel.remove(component)
      messageComponents.remove(id)
    }

    filtered.forEachIndexed { index, message ->
      val existingComponent = messageComponents[message.id]
      val component = if (existingComponent != null) {
        existingComponent
      }
      else {
        val newComponent = createMessageBubble(message)
        messageComponents[message.id] = newComponent as MessageBubble<ChatMessage>
        val comp = wrapWithBorder(newComponent, JBUI.Borders.empty(8, 0))
        comp.putClientProperty("messageId", message.id)
        messagesPanel.add(comp)
        newComponent
      }
      component.updateContent(message)
    }

    revalidate()
    repaint()
  }

  private fun createMessageBubble(message: ChatMessage): JComponent = when (message) {
    is UserMessage -> EditableUserMessageBubble(
      project = project,
      onCancelClick = {
        viewModel.cancelEditing(message.id)
      },
      onSendClick = {
        viewModel.updateMessage(message.id, it)
      }
    ) {
      viewModel.startEditing(message.id)
    }
    is AssistantMessage -> EditableAssistantUserMessageBubble(
      project = project,
      onCancelClick = {
        viewModel.cancelEditing(message.id)
      },
      onSendClick = {
        viewModel.updateMessage(message.id, it)
      },
      onEditClick = {
        viewModel.startEditing(message.id)
      },
      onRegenerateClick = {
        viewModel.regenerateLastResponse(message)
      },
      coroutineScope
    )
    else -> JPanel() // SystemMessage is not rendered here; it's represented by the header at the top
  }
}