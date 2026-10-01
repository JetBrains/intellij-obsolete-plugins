package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.asSafely
import com.intellij.dataWrangler.impl.action.setDataWranglerActionState
import com.intellij.dataWrangler.impl.fus.DataWranglerProviderCollector
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.JupyterPyDataWranglerNotebookContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataWranglerJupyterCellExport : AnAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = false
    val session = e.getData(DW_SESSION)?.asSafely<JupyterPyDataWranglerNotebookContext>() ?: return
    val name = session.getContext().getNotebookName()
    if (name == null) {
      return
    }
    setDataWranglerActionState(e)
    e.presentation.text = DataWranglerJupyterPyBundle.message("action.DataWrangler.Jupyter.Code.Export.text", name)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION)?.asSafely<JupyterPyDataWranglerNotebookContext>() ?: return
    e.coroutineScope.launch(Dispatchers.EDT) {
      exportCell(session)
    }
  }

  private suspend fun exportCell(session: DataWranglerSession<JupyterPyDataWranglerNotebookContext>) {
    val context = session.getContext()
    val stepsManager = session.getTransformationStepsManager()
    val generatedCode = session.getCodePreviewProvider()?.getTransformationCode(context, stepsManager.getExecutedCommands()) ?: return
    // keep until DW cells are stable
    context.addCodeCellAndNavigate(generatedCode)
    DataWranglerProviderCollector.logDWCodeExport(session.getTransformationStepsManager().getExecutedCommands())
  }

}