package com.intellij.dataWrangler.jupyterPython.engine.console

import com.intellij.dataWrangler.impl.database.DataWranglerLocalDatabase
import com.intellij.openapi.project.Project
import com.intellij.scientific.py.tables.commands.PyDebugProtocolTableCommandExecutor
import com.intellij.scientific.tables.api.DSDataFrameInfo
import com.intellij.scientific.tables.api.DSTableDataRetrieverFromDataSource
import com.jetbrains.python.console.PydevConsoleCommunication

internal class PyDataWranglerDatabaseContext(
  private val project: Project,
  private val pyFrameAccessor: PydevConsoleCommunication,
  private val tableDataRetriever: DSTableDataRetrieverFromDataSource,
  private val database: DataWranglerLocalDatabase,
) : PyConsoleDataWranglerContext {

  @Volatile
  private var dataFrameInfo = tableDataRetriever.dataFrameInfo
  override fun getPyFrameAccessor(): PydevConsoleCommunication = pyFrameAccessor
  override fun getDSDataFrameInfo(): DSDataFrameInfo = dataFrameInfo
  override fun getProject(): Project = project

  // Todo refactor setter
  override suspend fun updateTableInfo() {
    dataFrameInfo = tableDataRetriever.getTableDataProvider().loadDynamicTableDataFrameInfo(PyDebugProtocolTableCommandExecutor(pyFrameAccessor), getTableName(), "")
  }

  fun getDatabaseConfig(): DataWranglerLocalDatabase = database

  override fun getTableName(): String = tableDataRetriever.currentTableExpression
}