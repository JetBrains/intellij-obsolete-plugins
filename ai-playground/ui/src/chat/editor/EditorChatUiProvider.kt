package com.intellij.aiplayground.ui.chat.editor

import com.intellij.aiplayground.models.chat.Chat
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorChatUiProvider(val project: Project) : ChatUiProvider {
  override suspend fun openChat(chat: Chat): ChatViewModel? {
    return withContext(Dispatchers.EDT) {
      val chatFile = ChatVirtualFileHolder.getInstance(project).getOrCreate(chat)
      val fileEditorManager = FileEditorManagerEx.getInstanceExAsync(project)
      val options = FileEditorOpenOptions(reuseOpen = true, isSingletonEditorInWindow = true, requestFocus = true)
      fileEditorManager.openFile(chatFile, options).allEditors.filterIsInstance<ChatFileEditor>().firstOrNull()?.viewModel
    }
  }
}