package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.jupyter.core.core.impl.actions.NotebookCellsContents
import com.intellij.jupyter.py.actions.convert.JupyterPyScriptConverter
import com.intellij.lang.Language
import com.intellij.notebooks.jupyter.core.jupyter.JupyterLanguage
import com.intellij.notebooks.visualization.NotebookCellLinesProvider
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

private const val CODE_CELL_START = "#%%\n"

internal class DataWranglerExportNotebookAction : DataWranglerExportCodeAction() {

  private suspend fun <C : PythonDataWranglerContext> getNotebookContent(session: DataWranglerSession<C>): NotebookCellsContents? {
    val context = session.getContext()
    val previewProvider = session.getCodePreviewProvider() ?: return null
    val commands = session.getTransformationStepsManager().getExecutedCommands()
    val notebookContent = "${CODE_CELL_START}${previewProvider.getTransformationCode(context, commands)}"
    return readAction {
      val document = DocumentImpl(notebookContent)
      val intervals = NotebookCellLinesProvider.forLanguage(getTargetLanguage())?.makeIntervals(document) ?: return@readAction null
      NotebookCellsContents(notebookContent, intervals, language = getTargetLanguage())
    }
  }

  override fun getTargetLanguage(): Language = JupyterLanguage

  override suspend fun <C : PythonDataWranglerContext> getFileContent(session: DataWranglerSession<C>): String? {
    val content = getNotebookContent(session) ?: return null
    return JupyterPyScriptConverter.createJupyterNotebookContent(content, getTargetLanguage())
  }

  override fun writeFile(outputFile: VirtualFile, project: Project, content: String) {
    JupyterPyScriptConverter.createJupyterNotebook(outputFile, content)
  }
}