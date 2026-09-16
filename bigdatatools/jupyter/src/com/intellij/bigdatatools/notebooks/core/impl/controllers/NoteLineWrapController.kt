package com.intellij.bigdatatools.notebooks.core.impl.controllers

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.style.NoteStyleSettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.SoftWrapChangeListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key

object NoteLineWrapController {

  private val LINE_WRAP_CONTROLLER_LISTENER_KEY = Key<SoftWrapChangeListener>("WrapControllerListener")

  fun updateAllEditors() {
    EditorFactory.getInstance().allEditors.forEach {
      if (FileDocumentManager.getInstance().getFile(it.document) !is NotebookVirtualFile) return@forEach
      updateEditor(it)
    }
  }

  fun updateEditor(editor: Editor) {
    editor.settings.isUseSoftWraps = NoteStyleSettings.getInstance().editorSoftWraps

    if (editor !is EditorEx) {
      return
    }

    var listener = editor.getUserData(LINE_WRAP_CONTROLLER_LISTENER_KEY)

    if (listener != null) {
      return
    }

    listener = object : SoftWrapChangeListener {
      override fun softWrapsChanged() {
        NoteStyleSettings.getInstance().editorSoftWraps = editor.softWrapModel.isSoftWrappingEnabled
      }

      override fun recalculationEnds() = Unit
    }
    editor.softWrapModel.addSoftWrapChangeListener(listener)
    editor.putUserData(LINE_WRAP_CONTROLLER_LISTENER_KEY, listener)
  }
}