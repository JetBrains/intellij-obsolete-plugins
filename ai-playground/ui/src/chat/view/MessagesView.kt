package com.intellij.aiplayground.ui.chat.view

import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import javax.swing.JComponent

class MessagesView(
  viewModel: ChatViewModel,
  parentScope: CoroutineScope,
  project: Project,
) : JComponent() {

  private val chatScreenComponent = ChatScreenComponent(viewModel, parentScope, project)

  init {
    layout = BorderLayout()
    border = JBUI.Borders.empty(8)
    add(chatScreenComponent, BorderLayout.CENTER)
  }

  fun getPreferredFocusedComponent(): JComponent {
    return chatScreenComponent.getPreferredFocusedComponent()
  }

}
