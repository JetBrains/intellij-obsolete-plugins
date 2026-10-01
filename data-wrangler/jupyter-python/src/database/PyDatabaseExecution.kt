package com.intellij.dataWrangler.jupyterPython.database

import com.intellij.dataWrangler.impl.database.DataFrame
import com.intellij.dataWrangler.impl.database.DataWranglerLocalDatabase
import com.intellij.dataWrangler.impl.database.createSession
import com.intellij.dataWrangler.impl.database.getFormattedByteArrayWithTableData
import com.intellij.dataWrangler.jupyterPython.engine.DataWranglerPythonConsoleManager
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngine
import com.intellij.dataWrangler.jupyterPython.engine.getOrCreateTable
import com.intellij.database.datagrid.GridPanel.ViewPosition
import com.intellij.database.model.DasObject
import com.intellij.jupyter.core.jupyter.connections.execution.JupyterCodeWrapper
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewFileEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

fun getDWPyTableName(table: DasObject): String = table.name.let { "__${it}__" }

suspend fun uploadTableToPython(project: Project, localDatabase: DataWranglerLocalDatabase) {
  val consoleManager = project.service<DataWranglerPythonConsoleManager>()

  suspend fun sentDataAsString(dataFrames: IndexedValue<List<DataFrame>>) {
    val bytes = getFormattedByteArrayWithTableData(dataFrames.value, project, localDatabase)
    val code = JupyterCodeWrapper.addOrCreateTableDataFrame(localDatabase.tableName, bytes, dataFrames.index == 0)
    consoleManager.createAndWaitForTask(code)
  }

  suspend fun openTable() {
    val file = consoleManager.createDataViewVirtualFile(localDatabase.tableName, null) ?: return
    withContext(Dispatchers.EDT) {
      val fileEditors = FileEditorManager.getInstance(project).openEditor(OpenFileDescriptor(project, file), true)
      val dataViewFileEditor = fileEditors.firstOrNull { it is DataViewFileEditor } as? DataViewFileEditor ?: return@withContext
      val newGrid = dataViewFileEditor.dataViewerPanel.gridMutableStateFlow.first { it != null } ?: return@withContext
      val pyFrame = consoleManager.getPyFrameAccessor() ?: return@withContext
      val engine = PythonDataWranglerEngine()
      val context = engine.createPythonDatabaseConsoleContext(newGrid, pyFrame, localDatabase) ?: return@withContext
      val dwPanel = getOrCreateTable(newGrid, engine, context)
      newGrid.panel.putSideView(dwPanel, ViewPosition.RIGHT, null)
    }
  }

  createSession(project, localDatabase, ::sentDataAsString, ::openTable)
}