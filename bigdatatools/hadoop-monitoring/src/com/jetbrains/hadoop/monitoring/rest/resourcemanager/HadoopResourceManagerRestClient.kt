/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.hadoop.monitoring.rest.resourcemanager

import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.JDOMUtil
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService.createIfRequired
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringRestClient
import com.jetbrains.bigdatatools.common.rfs.util.withPrefixSlash
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ActivitiesInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppActivitiesInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppAttemptInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppAttemptsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppQueue
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppTimeoutInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppTimeoutsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ApplicationStatisticsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ApplicationTimeoutType
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterMetricsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterUserInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ContainerInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ContainersInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.HadoopConfigurationProperty
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogFileInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeLabel
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodesInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.RMQueueAclInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ReservationInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ReservationListInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.SchedulerTypeInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.toConnectionConfig
import org.jsoup.Jsoup

class HadoopResourceManagerRestClient(project: Project?,
                                      val connectionData: HadoopConnectionData,
                                      private val isTest: Boolean) : MonitoringRestClient(project,
                                                                                          connectionData.operationTimeout?.toIntOrNull()?.times(
                                                                                            1000)
                                                                                          ?: BdIdeRegistryUtil.RFS_DEFAULT_TIMEOUT) {

  override fun createConfig(): ConnectionConfig =
    connectionData.toConnectionConfig()

  override fun dispose() {}

  override fun connectInner(calledByUser: Boolean) {
    super.connectInner(calledByUser)
    createIfRequired(project, connectionData.getTunnelData(), connectionData.uri, connectionData.innerId, isTest)
      ?.let { tunnelHandler ->
        Disposer.register(this, tunnelHandler)
        restClient.url = tunnelHandler.tunnelledUri
      }

  }

  override fun checkConnectionInner() {
    getClusterInfo()
  }


  fun getToolsConfiguration(): List<HadoopConfigurationProperty> {
    val s = restClient.performGetRequest("/conf")
    // The response comes from a remote, potentially untrusted ResourceManager, so parse it with the
    // platform's hardened loader (DOCTYPE and external entities disabled) to prevent XXE (IJPL-249811).
    val rootElement = JDOMUtil.load(s)

    return rootElement.children.map {
      val name = it.getChild("name")?.text ?: ""
      val value = it.getChild("value")?.text ?: ""
      val final = it.getChild("final")?.text?.toBoolean() ?: false
      val source = it.getChild("source")?.text ?: ""
      HadoopConfigurationProperty(name, value, final, source)
    }
  }

  fun getToolsStacks(): String = restClient.performGetRequest("/stacks")
  fun getToolsMetrics(): String = restClient.performGetRequest("/jmx?qry=Hadoop:*")

  fun getLogsList(folder: String = "/logs"): List<LogFileInfo> {
    val domString = restClient.performGetRequest(folder)
    val document = Jsoup.parse(domString)
    val table = document.body().children()[1] ?: return emptyList()

    val rows = table.getElementsByTag("tbody").firstOrNull()?.children() ?: return emptyList()
    return rows.map {
      val columns = it.children()
      val name = columns[0].text()
      val path = columns[0].child(0).attr("href")
      val size = columns[1].text()
      val time = columns[2].text()
      LogFileInfo(path, name, size, time)
    }
  }

  fun getLogFileContent(path: String): String {
    return restClient.performGetRequest(path.withPrefixSlash())
  }

  @Suppress("unused")
  fun getApp(appId: String, unselectedFields: Set<String>?): AppInfo? {
    val uri = RMWSConsts.APPS_APPID.format(appId).withQuery(RMWSConsts.DESELECTS to unselectedFields?.joinToString())
    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMap(json, AppInfo::class.java).getValue()
  }

  /**
   * Unstable
   */
  fun getClusterUserInfo(): ClusterUserInfo? {
    val json = restClient.performGetRequest(RMWSConsts.CLUSTER_USER_INFO)
    return RmParser.parseClassMap(json, ClusterUserInfo::class.java).getValue()
  }


  fun getAppState(appId: String): YarnApplicationState? {
    val request = RMWSConsts.APPS_APPID_STATE.format(appId)
    val json = restClient.performGetRequest(request)
    return RmParser.parseClassMap(json, YarnApplicationState::class.java).getValue()
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  fun listReservation(reservationId: String?,
                      startTime: Long,
                      endTime: Long,
                      includeResourceAllocations: Boolean): List<ReservationInfo> = withNullOn404 {
    val json = restClient.performGetRequest(RMWSConsts.RESERVATION_LIST)
    RmParser.parseClassMap(json, ReservationListInfo::class.java).getValue()?.reservations
  } ?: emptyList()

  fun getAppTimeouts(appId: String): List<AppTimeoutInfo> = withNullOn404 {
    val json = restClient.performGetRequest(RMWSConsts.APPS_TIMEOUTS.format(appId))
    RmParser.parseClassMap(json, AppTimeoutsInfo::class.java).getValue()?.timeouts
  } ?: emptyList()

  @Suppress("unused", "UNUSED_PARAMETER")
  fun updateApplicationTimeout(appTimeout: AppTimeoutInfo, appId: String) {
    restClient.performGetRequest(RMWSConsts.APPS_TIMEOUT.format(appId))
  }

  fun getNode(nodeId: String): NodeInfo? {
    val json = restClient.performGetRequest(RMWSConsts.NODES_NODEID.format(nodeId))
    return RmParser.parseClassMap(json, NodeInfo::class.java).getValue()
  }

  fun updateAppQueue(appId: String, targetQueue: AppQueue): AppQueue {
    val body = mapOf("queue" to targetQueue.queue)
    val json = restClient.performPutData(RMWSConsts.APPS_APPID_QUEUE.format(appId), body)
    return BdtJson.fromJsonToClass(json, AppQueue::class.java)
  }

  fun getSchedulerInfo(): SchedulerTypeInfo {
    val json = restClient.performGetRequest(RMWSConsts.SCHEDULER)
    return RmParser.parseClassMapRemoveDublicatesMap(json, SchedulerTypeInfo::class.java).getValue() ?: error("Cannot get scheduler")
  }

  fun getAppStatistics(stateQueries: Set<String>? = null, typeQueries: Set<String>? = null): ApplicationStatisticsInfo {
    val uri = RMWSConsts.APP_STATISTICS.withQuery(
      RMWSConsts.STATES to stateQueries?.joinToString(),
      RMWSConsts.APPLICATION_TYPES to typeQueries?.joinToString()
    )

    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMap(json, ApplicationStatisticsInfo::class.java).getValue() ?: error("Cannot get appStatInfo")
  }

  @Suppress("unused")
  fun checkUserAccessToQueue(queue: String, username: String, queueAclType: String): RMQueueAclInfo? {
    val uri = RMWSConsts.CHECK_USER_ACCESS_TO_QUEUE.format(queue).withQuery(
      RMWSConsts.USER to username,
      RMWSConsts.QUEUE_ACL_TYPE to queueAclType
    )
    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMap(json, RMQueueAclInfo::class.java).getValue()
  }

  @Suppress("unused")
  fun getActivities(nodeId: String?): ActivitiesInfo? {
    val uri = RMWSConsts.SCHEDULER_ACTIVITIES.withQuery(RMWSConsts.NODEID to nodeId)
    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMap(json, ActivitiesInfo::class.java).getValue()
  }

  fun getClusterMetricsInfo(): ClusterMetricsInfo {
    val json = restClient.performGetRequest(RMWSConsts.METRICS)
    return RmParser.parseClassMap(json, ClusterMetricsInfo::class.java).getValue() ?: error("Cannot get clusterMetrics")
  }

  fun getContainers(appId: String?, appAttemptId: Int?): List<ContainerInfo> = withNullOn404 {
    val uri = RMWSConsts.APPS_APPID_APPATTEMPTS_APPATTEMPTID_CONTAINERS.format(appId, appAttemptId)
    val json = restClient.performGetRequest(uri)

    RmParser.parseClassMap(json, ContainersInfo::class.java).getValue()?.containers
  } ?: emptyList()

  fun getContainer(appId: String?, appAttemptId: Int?, containerId: String): ContainerInfo? = withNullOn404 {
    val uri = RMWSConsts.GET_CONTAINER.format(appId, appAttemptId, containerId)
    val json = restClient.performGetRequest(uri)

    RmParser.parseClassMap(json, ContainerInfo::class.java).getValue()
  }

  fun getClusterInfo(): ClusterInfo {
    val json = restClient.performGetRequest(RMWSConsts.INFO)
    return RmParser.parseClassMap(json, ClusterInfo::class.java).getValue() ?: error("Cannot get ClusterInfo")
  }

  @Suppress("unused")
  fun dumpSchedulerLogs(time: String) {
    restClient.performPostForm(RMWSConsts.SCHEDULER_LOGS, data = mapOf(RMWSConsts.TIME to time))
  }

  @Suppress("unused")
  fun getAppQueue(appId: String): AppQueue? = withNullOn404 {
    val uri = RMWSConsts.APPS_APPID_QUEUE.format(appId)
    val json = restClient.performGetRequest(uri)

    RmParser.parseClassMap(json, AppQueue::class.java).getValue()
  }

  @Suppress("unused")
  fun getAppActivities(appId: String?, time: String?): AppActivitiesInfo? {
    val uri = RMWSConsts.SCHEDULER_APP_ACTIVITIES.withQuery(RMWSConsts.APP_ID to appId, RMWSConsts.MAX_TIME to time)
    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMap(json, AppActivitiesInfo::class.java).getValue()
  }

  @Suppress("unused")
  fun getAppTimeout(appId: String, type: ApplicationTimeoutType): AppTimeoutInfo? {
    val json = restClient.performGetRequest(RMWSConsts.APPS_TIMEOUTS_TYPE.format(appId, type))
    return RmParser.parseClassMap(json, AppTimeoutInfo::class.java).getValue()
  }

  fun getApps(statesQuery: String? = null,
              finalStatusQuery: String? = null,
              user: String? = null,
              queue: String? = null,
              limit: Int? = null,
              startedBegin: String? = null,
              startedEnd: String? = null,
              finishBegin: String? = null,
              finishEnd: String? = null,
              applicationTypes: Set<String>? = null,
              applicationTags: Set<String>? = null,
              unselectedFields: Set<String>? = null): List<AppInfo> {
    val uri = RMWSConsts.APPS.withQuery(
      RMWSConsts.STATES to statesQuery,
      RMWSConsts.FINAL_STATUS to finalStatusQuery,
      RMWSConsts.USER to user,
      RMWSConsts.QUEUE to queue,
      RMWSConsts.LIMIT to limit?.toString(),
      RMWSConsts.STARTED_TIME_BEGIN to startedBegin,
      RMWSConsts.STARTED_TIME_END to startedEnd,
      RMWSConsts.FINISHED_TIME_BEGIN to finishBegin,
      RMWSConsts.FINISHED_TIME_END to finishEnd,
      RMWSConsts.APPLICATION_TYPES to applicationTypes?.joinToString(),
      RMWSConsts.APPLICATION_TAGS to applicationTags?.joinToString(),
      RMWSConsts.DESELECTS to unselectedFields?.joinToString()
    )

    // In the case of empty app list, the result will be {"apps":null}
    val json = restClient.performGetRequest(uri)
    return RmParser.parseClassMapRemoveDublicatesMap(json, AppsInfo::class.java).getValue()?.app ?: emptyList()
  }

  fun getNodes(states: String? = null): List<NodeInfo> {
    val json = restClient.performGetRequest(RMWSConsts.NODES.withQuery(RMWSConsts.STATES to states))
    return RmParser.parseClassMap(json, NodesInfo::class.java).getValue()?.node ?: emptyList()
  }

  fun getNodeLabels(): List<NodeLabel> {
    val domString = restClient.performGetRequest("/cluster/nodelabels")
    val document = Jsoup.parse(domString)
    val completed = document.body().getElementById("nodelabels")
    val rows = completed?.getElementsByTag("tbody")?.firstOrNull()?.children() ?: return emptyList()
    return rows.map {
      val columns = it.children()
      val labelName = columns[0].text()
      val labelType = columns[1].text()
      val numOfActiveNMs = columns[2].text().toInt()
      val totalResourcesRow = columns[3].text()
      NodeLabel(labelName, labelType, numOfActiveNMs, totalResource = RmParser.parseResourceInfoFromString(totalResourcesRow))
    }
  }

  fun getAppAttempts(appId: String): List<AppAttemptInfo> {
    val json = restClient.performGetRequest(RMWSConsts.APPS_APPID_APPATTEMPTS.format(appId))
    return RmParser.parseClassMap(json, AppAttemptsInfo::class.java).getValue()?.appAttempt ?: emptyList()
  }


  fun updateAppState(appId: String, targetState: YarnApplicationState): YarnApplicationState? {
    val body = mapOf("state" to targetState.name)
    val json = restClient.performPutData(RMWSConsts.APPS_APPID_STATE.format(appId), body)
    return RmParser.parseClassMap(json, YarnApplicationState::class.java).getValue()
  }

  private fun <T> Map<String, T>.getValue() = this.entries.firstOrNull()?.value
}