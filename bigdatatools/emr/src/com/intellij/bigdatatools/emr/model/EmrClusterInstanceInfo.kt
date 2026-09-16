package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.emr.util.EmrLocalizedColumn
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterKey
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import software.amazon.awssdk.services.emr.model.Instance
import software.amazon.awssdk.services.emr.model.InstanceGroupType

data class EmrClusterInstanceInfo(val type: InstanceGroupType,
                                  val state: String,
                                  val id: String,
                                  val publicUrl: String,
                                  val privateUrl: String,
                                  @field:NoRendering
                                  val instance: Instance,
                                  val instanceType: String,
                                  val ec2InstanceId: String,
                                  val market: String) : RemoteInfo {
  companion object {
    val STATES_FILTER = FilterKey("states")
    val LIMIT_FILTER = FilterKey("limit")
    val TEXT_FILTER = FilterKey("filterText")

    val renderableColumns: List<EmrLocalizedColumn<EmrClusterInstanceInfo>> by lazy {
      listOf(
        EmrLocalizedColumn(EmrClusterInstanceInfo::type, "data.emr.cluster.instance.type"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::state, "data.emr.cluster.instance.state"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::id, "data.emr.cluster.instance.id"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::publicUrl, "data.emr.cluster.instance.publicUrl"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::privateUrl, "data.emr.cluster.instance.privateUrl"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::instanceType, "data.emr.cluster.instance.instanceType"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::ec2InstanceId, "data.emr.cluster.instance.ec2InstanceId"),
        EmrLocalizedColumn(EmrClusterInstanceInfo::market, "data.emr.cluster.instance.market")
      )
    }

    fun getFrom(info: Instance, type: InstanceGroupType) = EmrClusterInstanceInfo(
      type = type,
      state = info.status().stateAsString() ?: "",
      instanceType = info.instanceType() ?: "",
      ec2InstanceId = info.ec2InstanceId() ?: "",
      market = info.marketAsString() ?: "",
      publicUrl = info.publicDnsName() ?: "",
      privateUrl = info.privateIpAddress() ?: "",
      id = info.id() ?: "",
      instance = info)
  }
}