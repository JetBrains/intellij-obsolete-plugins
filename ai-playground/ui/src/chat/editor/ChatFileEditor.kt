// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.aiplayground.ui.chat.editor

import com.intellij.CommonBundle
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.aiplayground.ui.chat.view.ChatView
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts.TabTitle
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.cancel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

internal class ChatFileEditor(
  private val project: Project,
  private val chatFile: ChatVirtualFile,
) : FileEditor {

  private val userDataHolder = UserDataHolderBase()

  private val coroutineScope = service<AiPlaygroundCoroutine>().coroutineScope.childScope("ChatView")
  val viewModel = ChatViewModel.create(project, coroutineScope, chatFile.chat)
  private val myPanel: ChatView = createPanel(viewModel)

  private fun createPanel(viewModel: ChatViewModel): ChatView {
    return ChatView(project, coroutineScope, viewModel)
  }

  override fun getFile(): VirtualFile {
    return chatFile
  }

  override fun getComponent(): JComponent {
    return myPanel
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return myPanel.getPreferredFocusedComponent()
  }

  override fun getName(): @TabTitle String = CommonBundle.settingsTitle()

  override fun setState(state: FileEditorState) {}

  override fun isModified(): Boolean {
    return false
  }

  override fun isValid(): Boolean {
    return true
  }

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
  }

  override fun dispose() {
    coroutineScope.cancel()
  }

  override fun <T> getUserData(key: Key<T?>): T? {
    return userDataHolder.getUserData(key)
  }

  override fun <T> putUserData(key: Key<T?>, value: T?) {
    userDataHolder.putUserData(key, value)
  }
}
