package com.intellij.bigdatatools.emr.target

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.emr.model.EmrClusterState
import com.intellij.bigdatatools.emr.rfs.EmrDriver
import com.intellij.bigdatatools.emr.settings.EmrConnectionGroup
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.spark.submit.run.cluster.AddConnectionOption
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmrRemoteTargetProvider : RemoteTargetProvider {
  override val connectionGroups: List<ConnectionGroup> = listOf(EmrConnectionGroup())

  override fun createConnectionTarget(project: Project) = AddConnectionOption(
    EmrMessagesBundle.message("add.new.submit.connection.label"),
    BdtConnectionType.EMR.id,
    RemoteTargetType.ADD_EMR
  ) {
    ConnectionSettings.create(project, EmrConnectionGroup(), applyIfOk = true) != null
  }

  override suspend fun getTargetById(project: Project, id: RemoteTargetId): EmrRemoteTarget? {
    val driver = id.connectionId?.let { DriverManager.getDriverById(project, it) } as? EmrDriver ?: return null
    if (!driver.waitConnect()) return null
    val dataManager = driver.dataManager
    val clusterId = id.clusterId ?: return null

    val cluster = withContext(Dispatchers.IO) {
      try {
        dataManager.loadClusterById(clusterId)
      }
      catch (t: Throwable) {
        thisLogger().warn("Cannot load cluster details", t)
        null
      }
    } ?: return null
    return EmrRemoteTarget(project, dataManager, cluster)
  }

  override suspend fun getRemoteTargets(project: Project): List<EmrRemoteTarget> {
    val drivers = DriverManager.getDrivers(project).filterIsInstance<EmrDriver>().filter {
      it.waitConnect()
    }
    return drivers.flatMap { getTargetForDriver(project, it) }
  }

  private suspend fun getTargetForDriver(project: Project, driver: EmrDriver): List<EmrRemoteTarget> {
    try {
      val dataManager = driver.dataManager
      val clusters = withContext(Dispatchers.IO) {
        dataManager.loadClusters(states = listOf(EmrClusterState.RUNNING))
      }
      return clusters.mapNotNull {
        getTargetById(project, EmrRemoteTarget.createId(driver, it))
      }.filter {
        it.hasSpark()
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

