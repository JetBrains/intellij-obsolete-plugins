package com.intellij.dataWrangler.jupyterPython.operations.init

import com.intellij.dataWrangler.impl.database.DataFrame
import com.intellij.dataWrangler.impl.database.DataWranglerLocalDatabase
import com.intellij.dataWrangler.impl.database.createSession
import com.intellij.dataWrangler.impl.database.getFormattedByteArrayWithTableData
import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.console.PyDataWranglerDatabaseContext
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.jupyter.core.jupyter.connections.execution.JupyterCodeWrapper
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

class PythonDatabaseTableParams

class PythonDatabaseTableFactory : CommandFactoryBase<PythonDatabaseTableParams, PythonDataWranglerContext>(PythonDatabaseTableParams::class,
                                                                                                            DataWranglerJupyterPyBundle.messagePointer("data.wrangler.py.local.file.init.name")) {
  override fun createCommand(parameters: PythonDatabaseTableParams): DataWranglerCommand<PythonDataWranglerContext> = PythonDatabaseTableCommand()
}


class PythonDatabaseTableCommand : DataWranglerCommand<PythonDataWranglerContext> {

  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.py.local.file.init.label")
  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.py.local.file.init.description")

  override suspend fun execute(context: PythonDataWranglerContext) {
    if (context !is PyDataWranglerDatabaseContext) return
    val database = context.getDatabaseConfig()
    reloadTable(context.getProject(), database, context)
  }

  private suspend fun reloadTable(project: Project, localDatabase: DataWranglerLocalDatabase, context: PyDataWranglerDatabaseContext) {
    suspend fun sentDataAsString(dataFrames: IndexedValue<List<DataFrame>>) {
      val bytes = getFormattedByteArrayWithTableData(dataFrames.value, project, localDatabase)
      val code = JupyterCodeWrapper.addOrCreateTableDataFrame(context.getTableName(), bytes, dataFrames.index == 0)
      context.executeCommand(code)
    }
    createSession(project, localDatabase, ::sentDataAsString) {}
  }

  companion object {
    fun createStep(): TransformationStep<*, PythonDataWranglerContext> {
      return TransformationStep(PythonDatabaseTableFactory(), PythonDatabaseTableParams())
    }
  }
}