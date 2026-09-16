package com.intellij.bigdatatools.plugin.spark.arbitrary.target

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionGroup
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterDriver
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.spark.submit.run.cluster.AddConnectionOption
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CancellationException

class ArbitraryClusterTargetProvider : RemoteTargetProvider {
  override val connectionGroups: List<ConnectionGroup> = listOf(ArbitraryClusterConnectionGroup())

  override fun createConnectionTarget(project: Project) = AddConnectionOption(
    SparkMessagesBundle.message("add.new.arbitrary.cluster.submit.connection.label"),
    BdtConnectionType.ARBITRARY_CLUSTER.id,
    RemoteTargetType.ADD_ARBITRARY_CLUSTER
  ) {
    ConnectionSettings.create(project, ArbitraryClusterConnectionGroup(), applyIfOk = true) != null
  }

  override suspend fun getTargetById(project: Project, id: RemoteTargetId): ArbitraryClusterRemoteTarget? {
    val driver = id.connectionId?.let { DriverManager.getDriverById(project, it) } as? ArbitraryClusterDriver ?: return null
    if (!driver.waitConnect()) return null
    val dataManager = driver.dataManager

    return ArbitraryClusterRemoteTarget(project, dataManager)
  }

  override suspend fun getRemoteTargets(project: Project): List<ArbitraryClusterRemoteTarget> {
    val drivers = DriverManager.getDrivers(project).filterIsInstance<ArbitraryClusterDriver>().filter {
      it.waitConnect()
    }
    return drivers.flatMap { getTargetForDriver(project, it) }
  }

  private fun getTargetForDriver(project: Project, driver: ArbitraryClusterDriver): List<ArbitraryClusterRemoteTarget> {
    try {
      val dataManager = driver.dataManager
      return listOf(ArbitraryClusterRemoteTarget(project, dataManager))
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

