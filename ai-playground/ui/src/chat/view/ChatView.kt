package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.ui.actions.CHAT
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.OnePixelSplitter
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import javax.swing.JComponent

class ChatView(project: Project, coroutineScope: CoroutineScope, private val viewModel: ChatViewModel) : JComponent(), UiDataProvider {

  private val messagesView = MessagesView(viewModel, coroutineScope, project)

  init {
    layout = BorderLayout()
    val splitPane = object : OnePixelSplitter(false, 1.0f) {
      override fun reshape(x: Int, y: Int, w: Int, h: Int) {
        if (this.width == 0 && this.height == 0) {
          proportion = 1 - 360f / w.toFloat()
        }
        super.reshape(x, y, w, h)
      }
    }.apply {
      firstComponent = messagesView
      secondComponent = PropertiesView(project, coroutineScope, viewModel)
      dividerPositionStrategy = Splitter.DividerPositionStrategy.KEEP_SECOND_SIZE
    }
    add(splitPane, BorderLayout.CENTER)
  }

  override fun uiDataSnapshot(sink: DataSink) {
    sink[CHAT] = viewModel.activeChat.value
  }

  fun getPreferredFocusedComponent(): JComponent {
    return messagesView.getPreferredFocusedComponent()
  }
}