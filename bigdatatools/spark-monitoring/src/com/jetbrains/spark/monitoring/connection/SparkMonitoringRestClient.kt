package com.jetbrains.spark.monitoring.connection

import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService.createIfRequired
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringRestClient
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.ApplicationEnvironmentInfo
import com.jetbrains.spark.monitoring.data.ApplicationInfo
import com.jetbrains.spark.monitoring.data.ExecutorSummary
import com.jetbrains.spark.monitoring.data.JobData
import com.jetbrains.spark.monitoring.data.RDDStorageInfo
import com.jetbrains.spark.monitoring.data.SqlInfo
import com.jetbrains.spark.monitoring.data.StageData
import com.jetbrains.spark.monitoring.data.TaskData
import com.jetbrains.spark.monitoring.data.VersionInfo
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

class SparkMonitoringRestClient(project: Project?,
                                private val connectionData: SparkConnectionData,
                                private val testConnection: Boolean = true) : MonitoringRestClient(project,
                                                                                                   connectionData.operationTimeout?.toIntOrNull()?.times(
                                                                                                     1000)
                                                                                                   ?: BdIdeRegistryUtil.RFS_DEFAULT_TIMEOUT) {

  override fun createConfig(): ConnectionConfig =
    connectionData.getConnectionConfig()

  private val basePath = "/api/v1/"

  override fun dispose() {}

  override fun checkConnectionInner() {
    getVersion()
    connectionData.historyServer = checkIsHistory()
  }

  override fun connectInner(calledByUser: Boolean) {
    super.connectInner(calledByUser)
    createIfRequired(project, connectionData.getTunnelData(), connectionData.uri, connectionData.innerId, testConnection)
      ?.let { tunnelHandler ->
        Disposer.register(this, tunnelHandler)
        restClient.url = tunnelHandler.tunnelledUri
      }
  }

  private inline fun <reified T> restRequestJsonArray(uri: String): List<T> {
    return restExceptionHelper.wrapRest {
      val json = restClient.performGetRequest(uri)
      BdtJson.fromJsonArray(json, T::class.java)
    }
  }

  private inline fun <reified T> restRequestJson(uri: String, customTimeout: Int? = null): T {
    return restExceptionHelper.wrapRest {
      val json = restClient.performGetRequest(uri, customTimeout = customTimeout)
      BdtJson.fromJsonToClass(json, T::class.java)
    }
  }

  fun getApplication(appId: String): ApplicationInfo {
    val uri = "${basePath}applications/$appId"
    return restRequestJson(uri)
  }


  fun getApplications(status: String? = null,
                      minDate: String? = null,
                      maxDate: String? = null,
                      minEndDate: String? = null,
                      maxEndDate: String? = null,
                      limit: String? = null): List<ApplicationInfo> {
    val uri = "${basePath}applications".withQuery(
      "status" to status,
      "minDate" to minDate,
      "maxDate" to maxDate,
      "minEndDate" to minEndDate,
      "maxEndDate" to maxEndDate,
      "limit" to limit
    )
    return restRequestJsonArray(uri)
  }

  fun getLogsUrl(appId: String, attemptId: String?): String {
    val id = AppAttemptId(appId, attemptId)
    return "${restClient.url}${basePath}applications/$id/logs"
  }


  fun getJobs(appId: AppAttemptId): List<JobData> {
    return restRequestJsonArray("${basePath}applications/$appId/jobs")
  }

  fun getStages(appId: AppAttemptId): List<StageData> {
    return restRequestJsonArray("${basePath}applications/$appId/stages")
  }

  fun getExecutors(appId: AppAttemptId): List<ExecutorSummary> {
    return restRequestJsonArray("${basePath}applications/${appId}/allexecutors")
  }

  fun getTasks(appId: AppAttemptId, stageId: Int, attemptId: Int): List<TaskData> {
    return restRequestJsonArray("${basePath}applications/$appId/stages/$stageId/$attemptId/taskList")
  }

  fun getStorages(appId: AppAttemptId): List<RDDStorageInfo> {
    return restRequestJsonArray("${basePath}applications/$appId/storage/rdd")
  }

  fun getEnvironment(appId: AppAttemptId): ApplicationEnvironmentInfo {
    return restRequestJson("${basePath}applications/$appId/environment")
  }

  private fun getVersion(): VersionInfo {
    return restRequestJson("${basePath}version", customTimeout = 3000)
  }

  fun getSql(): List<SqlInfo> {
    val html = restExceptionHelper.wrapRest {
      restClient.performGetRequest("/SQL/")
    }
    return SparkHtmlParser.parseSql(html)
  }

  fun getHistorySql(appId: AppAttemptId): List<SqlInfo> {
    val html = restExceptionHelper.wrapRest {
      restClient.performGetRequest("/history/$appId/SQL/")
    }
    return SparkHtmlParser.parseSql(html)
  }

  private fun checkIsHistory(): Boolean {
    val html = restExceptionHelper.wrapRest {
      restClient.performGetRequest("")
    }
    return SparkHtmlParser.parseIsHistory(html)
  }

  fun getDAGForJob(appId: String, jobId: String): Pair<List<String>, List<String>> {
    val html = restClient.performGetRequest("/history/$appId/jobs/job/?id=$jobId")
    return SparkHtmlParser.parseDAG(html)
  }
}