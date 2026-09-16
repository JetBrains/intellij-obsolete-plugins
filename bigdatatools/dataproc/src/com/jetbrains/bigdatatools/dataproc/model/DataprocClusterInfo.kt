package com.jetbrains.bigdatatools.dataproc.model

import com.google.cloud.dataproc.v1.Cluster
import com.google.cloud.dataproc.v1.ClusterStatus
import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtClusterInfo
import com.jetbrains.bigdatatools.dataproc.rfs.DataprocRfsTreeNode
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocClusterStateRenderer
import com.jetbrains.bigdatatools.dataproc.util.DataprocLocalizedField
import java.util.Date
import javax.swing.Icon

data class DataprocClusterInfo(@field:NoRendering val cluster: Cluster) : BdtClusterInfo {
  val name: String = cluster.clusterName
  override val id: String = cluster.clusterUuid

  @CustomRendering(DataprocClusterStateRenderer::class)
  val state: ClusterStatus.State? = cluster.status?.state

  @NoRendering
  val status: String? = state?.name

  @NoRendering
  val icon: Icon?
    get() = DataprocRfsTreeNode.getIconForCluster(state)
  @field:NoRendering
  val bdtStatus = DataprocClusterState.getFor(this)

  @field:NoRendering
  override val isStopped: Boolean = bdtStatus != DataprocClusterState.RUNNING

  val created: Date? = cluster.statusHistoryList?.firstOrNull()?.stateStartTime?.seconds?.let {
    Date(it * 1000)
  }

  val zone = cluster.config?.gceClusterConfig?.zoneUri?.takeLastWhile { it != '/' } ?: ""
  val region = zone.removeSuffix("-" + zone.takeLastWhile { it != '-' })
  val totalWorkers = cluster.config?.workerConfig?.numInstances ?: 0
  val scheduledDeletion = let {
    val lifecycleConfig = cluster.config.lifecycleConfig
    lifecycleConfig.hasAutoDeleteTtl() || lifecycleConfig.hasAutoDeleteTime() ||
    lifecycleConfig.hasIdleDeleteTtl() || lifecycleConfig.hasIdleStartTime()
  }
  val stagingBucket = cluster.config?.configBucket ?: ""


  companion object {
    val STATES_FILTER = FilterKey("states")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<DataprocLocalizedField<DataprocClusterInfo>> by lazy {
      listOf(
        DataprocLocalizedField(DataprocClusterInfo::name, "data.clusterInfo.name"),
        DataprocLocalizedField(DataprocClusterInfo::id, "data.clusterInfo.id"),
        DataprocLocalizedField(DataprocClusterInfo::state, "data.clusterInfo.state"),
        DataprocLocalizedField(DataprocClusterInfo::created, "data.clusterInfo.created"),
        DataprocLocalizedField(DataprocClusterInfo::zone, "data.clusterInfo.zone"),
        DataprocLocalizedField(DataprocClusterInfo::region, "data.clusterInfo.region"),
        DataprocLocalizedField(DataprocClusterInfo::totalWorkers, "data.clusterInfo.totalWorkers"),
        DataprocLocalizedField(DataprocClusterInfo::scheduledDeletion, "data.clusterInfo.scheduledDeletion"),
        DataprocLocalizedField(DataprocClusterInfo::stagingBucket, "data.clusterInfo.stagingBucket"),
      )
    }

    fun getFrom(info: Cluster): DataprocClusterInfo = DataprocClusterInfo(cluster = info)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataprocClusterInfo

    if (name != other.name) return false
    if (id != other.id) return false
    if (status != other.status) return false
    if (bdtStatus != other.bdtStatus) return false
    if (created != other.created) return false
    if (zone != other.zone) return false
    if (region != other.region) return false
    if (totalWorkers != other.totalWorkers) return false
    if (scheduledDeletion != other.scheduledDeletion) return false
    if (stagingBucket != other.stagingBucket) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + id.hashCode()
    result = 31 * result + (status?.hashCode() ?: 0)
    result = 31 * result + bdtStatus.hashCode()
    result = 31 * result + (created?.hashCode() ?: 0)
    result = 31 * result + zone.hashCode()
    result = 31 * result + region.hashCode()
    result = 31 * result + totalWorkers
    result = 31 * result + scheduledDeletion.hashCode()
    result = 31 * result + stagingBucket.hashCode()
    return result
  }
}