package com.intellij.bigdatatools.notebooks.core.api

import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.getPsiFile
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import javax.swing.JComponent

object NotebookDataProvider {

  @JvmStatic
  fun wrapComponent(
    component: JComponent,
    project: Project?,
    editor: Editor,
    note: BasicNotebook?,
    cell: NotebookCell? = null,
    helpId: String? = null,
    hostEditor: Editor? = null,
  ): JComponent {
    return UiDataProvider.wrapComponent(component) { sink ->
      uiDataSnapshot(sink, project, editor, note, cell, helpId, hostEditor)
    }
  }

  @JvmStatic
  fun uiDataSnapshot(
    sink: DataSink,
    project: Project?,
    editor: Editor,
    note: BasicNotebook?,
    cell: NotebookCell? = null,
    helpId: String? = null,
    hostEditor: Editor? = null,
  ) {
    sink[CommonDataKeys.PROJECT] = project
    sink[CommonDataKeys.EDITOR] = editor
    sink[CommonDataKeys.HOST_EDITOR] = hostEditor
    sink[NotebookDataKeys.NOTE_EDITOR] = editor as? EditorImpl
    sink[NotebookDataKeys.NOTE] = note
    sink[NotebookDataKeys.NOTE_CELL] = cell
    sink[PlatformDataKeys.HELP_ID] = helpId
    sink.lazy(CommonDataKeys.PSI_FILE) { editor.getPsiFile() }
  }
}