package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.run.workflow.DatabricksWorkflowRunner
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile

internal open class DatabricksRunWorkflowAction : DatabricksAbstractRunAction() {
  override val supportedExtensions = setOf("py", "ipynb")

  override fun createRunner(project: Project, dataManager: DatabricksDataManager): DatabricksWorkflowRunner {
    return DatabricksWorkflowRunner(project, dataManager)
  }

  @NlsActions.ActionText
  override fun getActionName(virtualFile: VirtualFile) = DatabricksBundle.message("action.run.as.workflow.text", virtualFile.name)
}