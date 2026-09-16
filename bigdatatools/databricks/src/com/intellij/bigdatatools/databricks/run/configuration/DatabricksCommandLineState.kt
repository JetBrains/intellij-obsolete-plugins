package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.run.direct.DatabricksServerRunner
import com.intellij.bigdatatools.databricks.run.workflow.DatabricksWorkflowRunner
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlin.io.path.Path

internal class DatabricksCommandLineState(private val project: Project, private val configuration: DatabricksRunConfiguration) : RunProfileState {

  override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
    val dataManager = DatabricksDataManager.getInstance(configuration.configurationId, project) ?: return null
    val file = VirtualFileManager.getInstance().findFileByNioPath(Path(configuration.filePath)) ?: return null

    dataManager.actionWrapperSuspend {
      withBackgroundProgress(project, DatabricksBundle.message("task.run.task.as.workflow"), true) {
        val databricksRunner = if (configuration.mode == DatabricksRunMode.WORKFLOW)
          DatabricksWorkflowRunner(project, dataManager)
        else
          DatabricksServerRunner(project, dataManager)

        databricksRunner.run(file)
      }
    }

    DatabricksWorkflowRunner(project, dataManager)
    return null
  }
}