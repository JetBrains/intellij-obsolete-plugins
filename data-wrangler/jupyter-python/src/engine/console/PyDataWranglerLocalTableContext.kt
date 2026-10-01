package com.intellij.dataWrangler.jupyterPython.engine.console

import com.intellij.dataWrangler.jupyterPython.engine.getInitDataFrameCode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.scientific.py.tables.commands.PyDebugProtocolTableCommandExecutor
import com.intellij.scientific.tables.api.DSDataFrameInfo
import com.intellij.scientific.tables.api.DSTableDataRetrieverFromDataSource
import com.jetbrains.python.console.PydevConsoleCommunication

internal class PyDataWranglerLocalTableContext(
  private val project: Project,
  private val pyFrameAccessor: PydevConsoleCommunication,
  private val tableDataRetriever: DSTableDataRetrieverFromDataSource,
  private val localFile: DataWranglerLocalFile,
) : PyConsoleDataWranglerContext {

  @Volatile
  private var dataFrameInfo = tableDataRetriever.dataFrameInfo

  override fun getPyFrameAccessor(): PydevConsoleCommunication = pyFrameAccessor
  override fun getProject(): Project = project
  override fun getFile(): VirtualFile = localFile.tableVirtualFile
  override fun getDSDataFrameInfo(): DSDataFrameInfo = dataFrameInfo
  override fun getTableName(): String = tableDataRetriever.currentTableExpression

  override fun getInitializationCode(): String {
    return getInitDataFrameCode(project, getVariableCodePreviewName(), getFile())
  }

  override suspend fun updateTableInfo() {
    dataFrameInfo = tableDataRetriever.getTableDataProvider().loadDynamicTableDataFrameInfo(PyDebugProtocolTableCommandExecutor(pyFrameAccessor), getTableName(), "")
  }
}