package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.run.direct.DatabricksServerRunner
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile

internal open class DatabricksRunShellAction : DatabricksAbstractRunAction() {
  override val supportedExtensions = setOf("py")

  override fun createRunner(project: Project,
                            dataManager: DatabricksDataManager) = DatabricksServerRunner(project, dataManager)

  @NlsActions.ActionText
  override fun getActionName(virtualFile: VirtualFile) = DatabricksBundle.message("action.run.as.shell.text", virtualFile.name)
}