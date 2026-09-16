package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.emr.util.EmrClusterAppUtils
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtClusterInfo
import software.amazon.awssdk.services.emr.model.Cluster
import software.amazon.awssdk.services.emr.model.ClusterState
import java.util.Date

data class EmrClusterDetails(val cluster: Cluster,
                             val summary: EmrClusterSummary,
                             val configuration: EmrClusterConfiguration,
                             val network: EmrClusterNetwork) : BdtClusterInfo {
  override val id: String = cluster.id()
  override val isStopped: Boolean = cluster.status().state() in setOf(ClusterState.TERMINATING, ClusterState.TERMINATED,
                                                                      ClusterState.TERMINATED_WITH_ERRORS)

  @NoRendering
  val sshKeyName = cluster.ec2InstanceAttributes()?.ec2KeyName()?.ifBlank { null }

  val isInternal = cluster.masterPublicDnsName()?.ifBlank { null }?.contains("ec2.internal") != false

  fun getApps(): List<EmrClusterAppInfo> {
    val apps = cluster.applications()
    val appsInfo = apps.flatMap {
      EmrClusterAppUtils.getFor(it, this)
    } + EmrClusterAppInfo.createSftp(this)

    return appsInfo
  }

  companion object {
    fun fromCluster(cluster: Cluster): EmrClusterDetails {
      val summary = EmrClusterSummary(
        id = cluster.id() ?: "",
        name = cluster.name() ?: "",
        creationDate = cluster.status().timeline().creationDateTime()?.let { Date.from(it) },
        endDate = cluster.status().timeline().endDateTime()?.let { Date.from(it) },
        autoTermination = cluster.autoTerminate(),
        terminationProtected = cluster.terminationProtected(),
        tags = cluster.tags()?.joinToString { it.key() + "" + it.value() } ?: "",
        masterPublicDns = cluster.masterPublicDnsName() ?: "",
        stateChangeReason = cluster.status().stateChangeReason()?.message() ?: "",
      )

      val ec2InstanceAttributes = cluster.ec2InstanceAttributes()
      val network = EmrClusterNetwork(
        availabilityZone = ec2InstanceAttributes.ec2AvailabilityZone() ?: "",
        subnetId = ec2InstanceAttributes.ec2SubnetId() ?: "",
        autoScaleRole = cluster.autoScalingRole() ?: "",
      )

      val configuration = EmrClusterConfiguration(
        releaseLabel = cluster.releaseLabel() ?: "",
        applications = cluster.applications()?.joinToString { it.name() + " " + it.version() } ?: "",
        logUri = cluster.logUri() ?: "",
        customAmiId = cluster.customAmiId() ?: "",
      )

      return EmrClusterDetails(cluster, summary, configuration, network)
    }
  }
}