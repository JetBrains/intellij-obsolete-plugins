package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.CustomRendering
import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.emr.table.renderers.ClusterStateRenderer
import com.intellij.bigdatatools.emr.util.EmrLocalizedColumn
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtClusterInfo
import software.amazon.awssdk.services.emr.model.ClusterState
import software.amazon.awssdk.services.emr.model.ClusterSummary
import java.util.Date

data class EmrClusterInfo(val name: String = "",
                          override val id: String = "",
                          @field:CustomRendering(ClusterStateRenderer::class)
                          val state: ClusterState? = null,
                          val normalizedInstanceHours: Int = -1,
                          @field:DateRendering
                          val created: Date? = null,
                          @field:DateRendering
                          val finished: Date? = null,
                          @field:NoRendering
                          val origin: ClusterSummary) : BdtClusterInfo {
  @field:NoRendering
  override val isStopped: Boolean = state !in setOf(ClusterState.RUNNING, ClusterState.WAITING, ClusterState.BOOTSTRAPPING)

  companion object {
    val STATES_FILTER = FilterKey("states")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<EmrLocalizedColumn<EmrClusterInfo>> by lazy {
      listOf(
        EmrLocalizedColumn(EmrClusterInfo::name, "data.emr.cluster.info.name"),
        EmrLocalizedColumn(EmrClusterInfo::id, "data.emr.cluster.info.id"),
        EmrLocalizedColumn(EmrClusterInfo::state, "data.emr.cluster.info.state"),
        EmrLocalizedColumn(EmrClusterInfo::normalizedInstanceHours, "data.emr.cluster.info.normalizedInstanceHours"),
        EmrLocalizedColumn(EmrClusterInfo::created, "data.emr.cluster.info.created"),
        EmrLocalizedColumn(EmrClusterInfo::finished, "data.emr.cluster.info.finished")
      )
    }

    fun getFrom(info: ClusterSummary): EmrClusterInfo {
      val state = ClusterState.knownValues().find { it.name == info.status().stateAsString() }
      return EmrClusterInfo(name = info.name() ?: "",
                            id = info.id() ?: "",
                            state = state,
                            created = info.status()?.timeline()?.creationDateTime()?.let { Date.from(it) },
                            finished = info.status()?.timeline()?.endDateTime()?.let { Date.from(it) },
                            normalizedInstanceHours = info.normalizedInstanceHours() ?: -1,
                            origin = info)
    }
  }
}