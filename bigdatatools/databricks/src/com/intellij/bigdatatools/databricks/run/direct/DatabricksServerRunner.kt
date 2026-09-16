package com.intellij.bigdatatools.databricks.run.direct

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.model.isRunning
import com.intellij.bigdatatools.databricks.run.common.DatabricksAbstractRunner
import com.intellij.bigdatatools.databricks.run.common.DatabricksRunType
import com.intellij.bigdatatools.databricks.statistic.DatabricksFeatureCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import kotlinx.coroutines.delay

internal class DatabricksServerRunner(project: Project, dataManager: DatabricksDataManager) : DatabricksAbstractRunner(project, dataManager) {
  private val wrapper = DbDirectRunWrapper(project, dataManager)

  override suspend fun runPyFile(
    file: VirtualFile,
    rfsPath: RfsPath,
    cluster: ClusterInfoPresentable,
    args: List<String>,
    env: Map<String, Any>,
  ) {
    val command = wrapper.createPyWrapperText(rfsPath, args, env)

    val clusterId = cluster.id
    val contextId = dataManager.client.createExecutionContext("python", clusterId)
    val storage = dataManager.serverExecutionStorage
    storage.init(file.name, project, contextId, command)

    try {
      val start = System.currentTimeMillis()
      val commandId = dataManager.client.executeCommand(clusterId, contextId, "python", command)
      try {
        while (true) {
          val commandStatusResponse = dataManager.client.getExecutionContextStatus(clusterId, contextId, commandId)
          storage.updateResult(contextId, commandStatusResponse)
          val isRunning = commandStatusResponse.status.isRunning
          if (isRunning) {
            delay(1000)
          }

          if (!isRunning) {
            DatabricksFeatureCollector.serverCommandExecuted(commandStatusResponse, System.currentTimeMillis() - start)
            return
          }
        }
      }
      catch (t: Throwable) {
        dataManager.client.cancelCommand(clusterId, contextId, commandId)
        throw t
      }
    }
    catch (t: Throwable) {
      DatabricksFeatureCollector.serverCommandThrowException()
      storage.setError(contextId, t)
    }
    finally {
      dataManager.client.destroyExecutionContextStatus(contextId = contextId, clusterId = clusterId)
    }
  }

  override fun detectFileType(file: VirtualFile) = if (file.extension == "py") {
    DatabricksRunType.PY_FILE
  }
  else {
    null
  }


  override suspend fun runNotebook(
    file: VirtualFile,
    rfsPath: RfsPath,
    clusterInfoPresentable: ClusterInfoPresentable,
  ) = error("Is not supported")
}