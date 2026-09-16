package com.jetbrains.bigdatatools.dataproc.target

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState
import com.jetbrains.bigdatatools.dataproc.rfs.DataprocDriver
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionGroup
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.run.cluster.AddConnectionOption
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DataprocRemoteTargetProvider : RemoteTargetProvider {
  override val connectionGroups: List<ConnectionGroup> = listOf(DataprocConnectionGroup())

  override fun createConnectionTarget(project: Project) = AddConnectionOption(
    DataprocMessagesBundle.message("add.new.submit.connection.label"),
    BdtConnectionType.DATAPROC.id,
    RemoteTargetType.ADD_DATAPROC
  ) {
    ConnectionSettings.create(project, DataprocConnectionGroup(), applyIfOk = true) != null
  }

  override suspend fun getTargetById(project: Project, id: RemoteTargetId): DataprocRemoteTarget? {
    val driver = id.connectionId?.let { DriverManager.getDriverById(project, it) } as? DataprocDriver ?: return null
    if (!driver.waitConnect()) return null
    val dataManager = driver.dataManager
    val clusterId = id.clusterId ?: return null

    val cluster = withContext(Dispatchers.IO) {
      dataManager.loadCluster(clusterId)
    }
    val clusterInfo = DataprocClusterInfo(cluster)
    return DataprocRemoteTarget(project, dataManager, clusterInfo)
  }

  override suspend fun getRemoteTargets(project: Project): List<DataprocRemoteTarget> {
    val drivers = DriverManager.getDrivers(project).filterIsInstance<DataprocDriver>().filter {
      it.waitConnect()
    }
    return drivers.flatMap { getTargetForDriver(project, it) }
  }

  private suspend fun getTargetForDriver(project: Project, driver: DataprocDriver): List<DataprocRemoteTarget> {
    try {
      val dataManager = driver.dataManager
      val clusters = withContext(Dispatchers.IO) {
        dataManager.loadClusters(listOf(DataprocClusterState.RUNNING))
      }
      return clusters.map {
        DataprocRemoteTarget(project, dataManager, it)
      }
    }
    catch (ce: CancellationException) {
      throw ce
    }
    catch (t: Throwable) {
      thisLogger().warn("Cannot get spark cluster targets for ${driver.connectionData.name}", t)
      return emptyList()
    }
  }
}