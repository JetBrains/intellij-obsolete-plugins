package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager
import com.intellij.dataWrangler.impl.action.DataWranglerPanelToggleAction
import com.intellij.dataWrangler.impl.action.setDataWranglerActionState
import com.intellij.dataWrangler.impl.service.DataWranglerService
import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngine
import com.intellij.dataWrangler.jupyterPython.engine.getOrCreateTable
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.database.datagrid.GridPanel.ViewPosition
import com.intellij.jupyter.py.pro.actions.OpenDSTableInNewTab.Companion.openInTabDataViewVirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewFileEditor
import com.intellij.python.scientific.powerfuldataviewer.editor.DataViewVirtualFile
import com.intellij.scientific.tables.panel.DSTable
import com.intellij.scientific.tables.panel.DSTableWithStatistics
import com.intellij.scientific.tables.panel.DS_TABLE_DATA_KEY
import com.intellij.scientific.tables.panel.DS_TABLE_KEY
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataWranglerPyPanelToggleAction : DataWranglerPanelToggleAction()

//todo: separate action
interface DataWranglerOpenActionExtension {
  suspend fun open(dataContext: DataContext): Boolean

  companion object {
    private val EP_NAME = ExtensionPointName.create<DataWranglerOpenActionExtension>("com.intellij.dataWrangler.openActionExtension")

    suspend fun open(dataContext: DataContext): Boolean {
      return EP_NAME.extensionList.any { it.open(dataContext) }
    }
  }
}

class DataWranglerOpenAction : AnAction(), DumbAware {

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
    super.update(e)
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val dataContext = e.dataContext
    project.service<DataWranglerService>().coroutineScope.launch(Dispatchers.EDT) {
      if (!DataWranglerOpenActionExtension.open(e.dataContext)) {
        openFromCellWithTable(project, dataContext)
      }
    }
  }

  companion object {
    @RequiresEdt
    suspend fun openFromCellWithTable(project: Project, dataContext: DataContext) {
      val table: DSTable = dataContext.getData(DS_TABLE_DATA_KEY) ?: return

      val file = openInTabDataViewVirtualFile(project, table) ?: return

      val fileEditors = withContext(Dispatchers.EDT) {
        project.serviceAsync<FileEditorManager>().openEditor(OpenFileDescriptor(project, file), true)
      }

      val dataViewFileEditor = fileEditors.firstOrNull { it is DataViewFileEditor } as? DataViewFileEditor ?: return

      withContext(Dispatchers.Default) {
        openDataWranglerFromFile(dataViewFileEditor, file, listOf())
      }
    }

    suspend fun openDataWranglerFromFile(dataViewFileEditor: DataViewFileEditor, file: DataViewVirtualFile, currentSteps: List<TransformationStep<*, *>>): DWMainPanel? {
      return withContext(Dispatchers.IO) {
        val grid = dataViewFileEditor.dataViewerPanel.gridMutableStateFlow.first { it != null } ?: return@withContext null
        val dsTable = grid.getUserData(DS_TABLE_KEY) as DSTableWithStatistics
        val engine = PythonDataWranglerEngine()
        withContext(Dispatchers.EDT) {
          val context = engine.createJupyterContext(grid, file, dsTable) ?: return@withContext null
          val dwPanel = getOrCreateTable(grid, engine, context)
          val session = dwPanel.backendSession

          currentSteps.forEach { step ->
            addStep(step, session.getTransformationStepsManager())
          }
          if (currentSteps.isNotEmpty()) {
            session.rerunSession()
          }

          grid.panel.putSideView(dwPanel, ViewPosition.RIGHT, null)
          dwPanel
        }
      }
    }

  }
}


// workaround around generics
private fun <C : DataWranglerContext> addStep(step: TransformationStep<*, *>, manager: DataWranglerTransformationStepsManager<C>?) {
  val sameStep = step.asSafely<TransformationStep<*, C>>() ?: return
  manager?.addTransformation(sameStep)
}
