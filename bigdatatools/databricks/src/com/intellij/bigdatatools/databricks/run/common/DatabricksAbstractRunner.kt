package com.intellij.bigdatatools.databricks.run.common

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.reportProgress
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

internal abstract class DatabricksAbstractRunner(val project: Project, val dataManager: DatabricksDataManager) {
  suspend fun run(file: VirtualFile) = reportProgress(4) { progress ->
    val isSynced = progress.itemStep(DatabricksBundle.message("task.run.task.as.workflow.step.wait.sync")) {
      waitWorkspaceSync()
    }
    if (!isSynced)
      error("Workspace is not synced")

    val clusterInfoPresentable = progress.itemStep(DatabricksBundle.message("task.run.task.as.workflow.step.wait.cluster")) {
      waitClusterStart()
    }

    val rfsPath = dataManager.syncManager.getTaskForProject(project).syncMapper.localPathToRemotePath(file.toNioPath())

    when (detectFileType(file)) {
      DatabricksRunType.NOTEBOOK -> {
        progress.itemStep(DatabricksBundle.message("task.run.task.as.workflow.step.run.notebook")) {
          runNotebook(file, rfsPath, clusterInfoPresentable)
        }
      }
      DatabricksRunType.PY_FILE -> {
        progress.itemStep(DatabricksBundle.message("task.run.task.as.workflow.step.run.pyfile")) {
          runPyFile(file, rfsPath, clusterInfoPresentable)
        }
      }
      null -> {
        return@reportProgress
      }
    }
  }

  abstract suspend fun runPyFile(
    file: VirtualFile,
    rfsPath: RfsPath,
    cluster: ClusterInfoPresentable,
    args: List<String> = emptyList(),
    env: Map<String, Any> = emptyMap(),
  )

  abstract suspend fun runNotebook(
    file: VirtualFile,
    rfsPath: RfsPath,
    clusterInfoPresentable: ClusterInfoPresentable,
  )

  protected abstract fun detectFileType(file: VirtualFile): DatabricksRunType?

  private suspend fun waitClusterStart(): ClusterInfoPresentable {
    val cluster = dataManager.getCurrentCluster() ?: error("Cluster is not attached")
    if (!cluster.isClusterStarted()) {
      dataManager.startCluster(cluster)
    }

    val isReady = dataManager.waitClusterStart(cluster)
    if (!isReady) {
      error("Cluster ${cluster.name} is not ready")
    }

    val freshCluster = dataManager.loadCluster(cluster.id) ?: error("Cluster ${cluster.name} is not loaded")
    if (!freshCluster.isClusterReady()) {
      error("Cluster cannot be started")
    }
    return freshCluster
  }

  private suspend fun waitWorkspaceSync(): Boolean {
    val syncProjectTask = dataManager.syncManager.getTaskForProject(project)
    if (!syncProjectTask.status.isRunning()) {
      syncProjectTask.start()
    }
    return syncProjectTask.waitForSync()
  }
}