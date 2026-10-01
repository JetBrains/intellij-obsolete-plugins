package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.ide.CopyPasteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.datatransfer.StringSelection

class CopyDataWranglerCodeAction : AnAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION) ?: return
    copyCode(e.coroutineScope, session)
  }

  private fun <C : DataWranglerContext> copyCode(scope: CoroutineScope, session: DataWranglerSession<C>) {
    val context = session.getContext()
    val codePreviewProvider = session.getCodePreviewProvider() ?: return
    val commands = session.getTransformationStepsManager().getExecutedCommands()
    scope.launch(Dispatchers.EDT) {
      val textToCopy = codePreviewProvider.getTransformationCode(context, commands)
      val selection = StringSelection(textToCopy)
      CopyPasteManager.getInstance().setContents(selection)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }
}