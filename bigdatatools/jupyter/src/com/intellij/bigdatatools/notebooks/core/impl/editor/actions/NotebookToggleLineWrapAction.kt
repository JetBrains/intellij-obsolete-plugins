package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteLineWrapController
import com.intellij.bigdatatools.notebooks.style.NoteStyleSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareToggleAction

class NotebookToggleLineWrapAction : DumbAwareToggleAction(), RightAlignedToolbarAction {
  override fun isSelected(e: AnActionEvent) = NoteStyleSettings.getInstance().editorSoftWraps

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    NoteStyleSettings.getInstance().editorSoftWraps = !NoteStyleSettings.getInstance().editorSoftWraps
    NoteLineWrapController.updateAllEditors()
  }
}