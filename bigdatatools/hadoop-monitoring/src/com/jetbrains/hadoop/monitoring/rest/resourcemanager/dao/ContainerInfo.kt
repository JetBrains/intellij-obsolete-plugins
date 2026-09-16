/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.intellij.bigdatatools.coreUi.table.renderers.LinkRendering
import com.intellij.bigdatatools.coreUi.table.renderers.UnixtimeRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class ContainerInfo(
  val containerId: String? = null,
  val allocatedMB: Long = 0,
  val allocatedVCores: Long = 0,
  val assignedNodeId: String? = null,
  val priority: Int = 0,

  @field:UnixtimeRendering
  val startedTime: Long = 0,

  @field:UnixtimeRendering
  val finishedTime: Long = 0,

  @field:UnixtimeRendering
  val elapsedTime: Long = 0,

  val diagnosticsInfo: String? = null,

  @field:LinkRendering
  val logUrl: String? = null,
  val containerExitStatus: Int = 0,
  val containerState: ContainerState? = null,

  @field:LinkRendering
  val nodeHttpAddress: String? = null,
  val nodeId: String? = null,
  val allocatedResources: Map<String, Long>? = null
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<ContainerInfo>> by lazy {
      listOf(
        HadoopLocalizedField(ContainerInfo::containerId, "data.ContainerInfo.containerId"),
        HadoopLocalizedField(ContainerInfo::allocatedMB, "data.ContainerInfo.allocatedMB"),
        HadoopLocalizedField(ContainerInfo::allocatedVCores, "data.ContainerInfo.allocatedVCores"),
        HadoopLocalizedField(ContainerInfo::assignedNodeId, "data.ContainerInfo.assignedNodeId"),
        HadoopLocalizedField(ContainerInfo::priority, "data.ContainerInfo.priority"),
        HadoopLocalizedField(ContainerInfo::startedTime, "data.ContainerInfo.startedTime"),
        HadoopLocalizedField(ContainerInfo::finishedTime, "data.ContainerInfo.finishedTime"),
        HadoopLocalizedField(ContainerInfo::elapsedTime, "data.ContainerInfo.elapsedTime"),
        HadoopLocalizedField(ContainerInfo::diagnosticsInfo, "data.ContainerInfo.diagnosticsInfo"),
        HadoopLocalizedField(ContainerInfo::logUrl, "data.ContainerInfo.logUrl"),
        HadoopLocalizedField(ContainerInfo::containerExitStatus, "data.ContainerInfo.containerExitStatus"),
        HadoopLocalizedField(ContainerInfo::containerState, "data.ContainerInfo.containerState"),
        HadoopLocalizedField(ContainerInfo::nodeHttpAddress, "data.ContainerInfo.nodeHttpAddress"),
        HadoopLocalizedField(ContainerInfo::nodeId, "data.ContainerInfo.nodeId"),
        HadoopLocalizedField(ContainerInfo::allocatedResources, "data.ContainerInfo.allocatedResources")
      )
    }
  }
}