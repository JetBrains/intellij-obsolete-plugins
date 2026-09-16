package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.ide.actions.ToggleDistractionFreeModeAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager

class ZeppelinToggleDistractionFreeModeAction : ToggleDistractionFreeModeAction() {
  override fun update(e: AnActionEvent) {

    val editor = if (e.project == null) null else FileEditorManager.getInstance(e.project!!).selectedTextEditor as EditorImpl?
    e.presentation.isEnabledAndVisible = !(editor != null && editor.virtualFile is NotebookVirtualFile)

    super.update(e)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT
}