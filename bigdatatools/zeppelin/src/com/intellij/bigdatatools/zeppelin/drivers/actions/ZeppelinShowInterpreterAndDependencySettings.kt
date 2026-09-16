package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.interpreter.InterpreterAndDependencySettingsDialog
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent

class ZeppelinShowInterpreterAndDependencySettings : ZeppelinPaneAction(ZepMessagesBundle.message("note.settings.dialog.title"),
                                                                        AllIcons.General.GearPlain) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val zeppelinEditor = e.noteEditor?.getUserData(ZeppelinEditor.ZEPPELIN_EDITOR) ?: return
    InterpreterAndDependencySettingsDialog.show(project, zeppelinEditor)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}