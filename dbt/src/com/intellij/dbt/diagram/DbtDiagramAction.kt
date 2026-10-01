package com.intellij.dbt.diagram


import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction


class DbtDiagramAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val module = e.dataContext.getData(com.intellij.openapi.actionSystem.PlatformCoreDataKeys.MODULE) ?: return
    val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return

    openLineage(editor.virtualFile, module, forced = true)
  }
}
