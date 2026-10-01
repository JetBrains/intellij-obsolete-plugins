package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.impl.action.setDataWranglerActionState
import com.intellij.dataWrangler.impl.database.DataWranglerLocalDatabase
import com.intellij.dataWrangler.impl.database.generateDataWranglerQueryForRequest
import com.intellij.dataWrangler.jupyterPython.database.getDWPyTableName
import com.intellij.dataWrangler.jupyterPython.database.uploadTableToPython
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreatePythonContextFromDatabase
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils.canCreatePythonLocalTableContext
import com.intellij.dataWrangler.jupyterPython.engine.console.DataWranglerLocalFile
import com.intellij.dataWrangler.jupyterPython.engine.openLocalFileInDataWranglerPyConsole
import com.intellij.database.DatabaseDataKeys
import com.intellij.database.dataSource.localDataSource
import com.intellij.database.datagrid.DatabaseGridDataHookUp
import com.intellij.database.datagrid.GridUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.HtmlChunk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls

class DataWranglerOpenTableFileAction : AnAction(), DumbAware {
  init {
    templatePresentation.apply {
      putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      putClientProperty(ActionUtil.USE_SMALL_FONT_IN_TOOLBAR, true)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    e.coroutineScope.launch(Dispatchers.Default) {
      try {
        openDataWrangler(project, e.dataContext)
      }
      catch (c: CancellationException) {
        throw c
      }
      catch (th: Throwable) {
        showError(th.message ?: "", e)
      }
    }
  }

  private fun showError(@Nls text: String, e: AnActionEvent) {
    JBPopupFactory.getInstance().apply {
      createHtmlTextBalloonBuilder(HtmlChunk.text(text).toString(), MessageType.ERROR, null)
        .setFadeoutTime(10000)
        .createBalloon()
        .show(guessBestPopupLocation(this@DataWranglerOpenTableFileAction, e), Balloon.Position.below)
    }
  }

  companion object {
    suspend fun openDataWrangler(project: Project, dataContext: DataContext) {
      withContext(Dispatchers.Default) {
        when {
          canCreatePythonLocalTableContext(dataContext) -> {
            val grid = GridUtil.getDataGrid(dataContext) ?: return@withContext
            val tableFile = GridUtil.getVirtualFile(grid.dataHookup) ?: return@withContext
            openLocalFileInDataWranglerPyConsole(DataWranglerLocalFile(tableFile), project)
          }
          canCreatePythonContextFromDatabase(dataContext) -> {
            val dataGrid = dataContext.getData(DatabaseDataKeys.DATA_GRID_KEY) ?: return@withContext
            val loader = dataGrid.dataHookup as? DatabaseGridDataHookUp ?: return@withContext
            val localDataSource = loader.dataSource.localDataSource ?: return@withContext
            val table = loader.databaseTable ?: return@withContext
            val query = generateDataWranglerQueryForRequest(localDataSource.dbms, table)
            uploadTableToPython(project, DataWranglerLocalDatabase(localDataSource, query, getDWPyTableName(table)))
          }
        }
      }
    }
  }
}