package com.intellij.dataWrangler.impl.action

import com.intellij.dataWrangler.DW_SESSION
import com.intellij.dataWrangler.impl.fus.DataWranglerProviderCollector
import com.intellij.database.dump.ShowDumpDialogGridAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DataWranglerDataExportAction : ShowDumpDialogGridAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    setDataWranglerActionState(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val session = e.getData(DW_SESSION) ?: return
    DataWranglerProviderCollector.logDWCSVExport(session.getTransformationStepsManager().getExecutedCommands())
    super.actionPerformed(e)
  }
}