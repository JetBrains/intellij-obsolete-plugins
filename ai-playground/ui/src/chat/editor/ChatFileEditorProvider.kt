// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.aiplayground.ui.chat.editor

import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.utils.getChatName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile


class ChatFileEditorProvider : FileEditorProvider, EditorTabTitleProvider, DumbAware {
  companion object {
    const val ID = "AiPlaygroundChatFileEditor"
  }

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return file is ChatVirtualFile
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val chatFile = file as ChatVirtualFile
    PlaygroundCollector.logEditorOpened()
    return ChatFileEditor(project, chatFile)
  }

  override fun getEditorTypeId(): String {
    return ID
  }

  override fun getPolicy(): FileEditorPolicy {
    return FileEditorPolicy.HIDE_OTHER_EDITORS
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun disposeEditor(editor: FileEditor) {
    val disposable = editor as ChatFileEditor
    Disposer.dispose(disposable)
    (disposable.file as? ChatVirtualFile)?.let {
      PlaygroundCollector.logEditorClosed()
    }
  }

  override fun getEditorTabTooltipHtml(project: Project, virtualFile: VirtualFile): HtmlChunk? {
    if (virtualFile !is ChatVirtualFile) return null
    return HtmlChunk.text(getChatName(virtualFile.chat.title))
  }

  override fun getEditorTabTitle(project: Project, virtualFile: VirtualFile): @NlsContexts.TabTitle String? {
    if (virtualFile !is ChatVirtualFile)
      return null
    return getChatName(virtualFile.chat.title)
  }

}
