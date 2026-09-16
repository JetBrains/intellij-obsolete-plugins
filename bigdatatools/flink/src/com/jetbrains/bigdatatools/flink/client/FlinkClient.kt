package com.jetbrains.bigdatatools.flink.client

import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.connection.exception.impl.RestResponseException
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService.createIfRequired
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringRestClient
import com.jetbrains.bigdatatools.flink.model.JarInfo
import com.jetbrains.bigdatatools.flink.model.Jars
import com.jetbrains.bigdatatools.flink.model.JobCheckpoints
import com.jetbrains.bigdatatools.flink.model.JobCheckpointsConfig
import com.jetbrains.bigdatatools.flink.model.JobConfiguration
import com.jetbrains.bigdatatools.flink.model.JobDetailsInfo
import com.jetbrains.bigdatatools.flink.model.JobException
import com.jetbrains.bigdatatools.flink.model.JobExecutionConfig
import com.jetbrains.bigdatatools.flink.model.JobExecutionStatus
import com.jetbrains.bigdatatools.flink.model.JobInfo
import com.jetbrains.bigdatatools.flink.model.JobManagerConfig
import com.jetbrains.bigdatatools.flink.model.JobManagerLogs
import com.jetbrains.bigdatatools.flink.model.Jobs
import com.jetbrains.bigdatatools.flink.model.LogFileInfo
import com.jetbrains.bigdatatools.flink.model.TaskManagerInfo
import com.jetbrains.bigdatatools.flink.model.TaskManagers
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData

class FlinkClient(project: Project?,
                  private val connectionData: FlinkConnectionData,
                  private val testConnection: Boolean)
  : MonitoringRestClient(project,
                         connectionData.operationTimeout?.toIntOrNull()?.times(1000) ?: BdIdeRegistryUtil.RFS_DEFAULT_TIMEOUT) {

  override fun createConfig(): ConnectionConfig =
    connectionData.getConnectingConfig()

  override fun dispose() {}

  override fun checkConnectionInner() {
    getJars()
  }

  override fun connectInner(calledByUser: Boolean) {
    super.connectInner(calledByUser)

    createIfRequired(project, connectionData.getTunnelData(), connectionData.uri, connectionData.innerId, testConnection)
      ?.let { tunnelHandler ->
        Disposer.register(this, tunnelHandler)
        restClient.url = tunnelHandler.tunnelledUri
      }
  }

  fun getJars(filter: String? = null,
              limit: Int? = null): List<JarInfo> {
    val jars = try {
      val json = restClient.performGetRequest("/jars")
      BdtJson.fromJsonToClass(json, Jars::class.java).files
    }
    catch (t: RestResponseException) {
      if (t.actualCode == 404)
        emptyList()
      else
        throw t
    }

    val filterRegex = filter?.let { Regex(it) }
    val filteredJars = if (filterRegex != null) {
      jars.filter { (it.name + it.entryClass).contains(filterRegex) }
    }
    else {
      jars
    }
    return if (limit != null && limit >= 0) filteredJars.take(limit) else filteredJars
  }

  fun getJobs(statuses: List<JobExecutionStatus>? = null,
              filter: String? = null,
              limit: Int? = null): List<JobInfo> {
    val json = restClient.performGetRequest("/jobs/overview")
    val jobs = BdtJson.fromJsonToClass(json, Jobs::class.java).jobs

    val jobsWithStatuses = if (statuses != null && statuses.size < JobExecutionStatus.entries.size) {
      jobs.filter { it.status in statuses }
    }
    else {
      jobs
    }

    val filterRegex = filter?.let { Regex(it) }
    val filteredJobs = if (filterRegex != null) {
      jobsWithStatuses.filter { (it.jobName + it.status).contains(filterRegex) }
    }
    else {
      jobsWithStatuses
    }
    return if (limit != null && limit >= 0) filteredJobs.take(limit) else filteredJobs
  }

  fun cancelJob(jobId: String) {
    restClient.performPatchRequest("/jobs/$jobId")
  }

  fun uploadJar(pathToJarFile: String) {
    restClient.performPostJarFile("/jars/upload", pathToJarFile)
  }

  fun runJar(jarId: String, data: Map<String, Any?>) {
    restClient.performPostData("/jars/$jarId/run", data, emptyMap())
  }

  fun deleteJar(jarId: String) {
    restClient.performDeleteData("/jars/$jarId", emptyMap())
  }

  fun getDetailsOfJob(jobId: String): JobDetailsInfo {
    val json = restClient.performGetRequest("/jobs/$jobId")
    return BdtJson.fromJsonToClass(json, JobDetailsInfo::class.java)
  }

  fun getJobException(jobId: String): JobException {
    val json = restClient.performGetRequest("/jobs/$jobId/exceptions")
    return BdtJson.fromJsonToClass(json, JobException::class.java)
  }

  fun getJobCheckpoints(jobId: String): JobCheckpoints? {
    val json = hasJobCheckpoints { restClient.performGetRequest("/jobs/$jobId/checkpoints") } ?: return null
    return BdtJson.fromJsonToClass(json, JobCheckpoints::class.java)
  }

  fun getJobCheckpointsConfig(jobId: String): JobCheckpointsConfig? {
    val json = hasJobCheckpoints { restClient.performGetRequest("/jobs/$jobId/checkpoints/config") } ?: return null
    return BdtJson.fromJsonToClass(json, JobCheckpointsConfig::class.java)
  }

  private fun hasJobCheckpoints(performRequest: () -> String): String? = try {
    performRequest()
  }
  catch (restException: RestResponseException) {
    if (restException.actualCode == 404)
      null
    else
      throw restException
  }

  fun getJobConfig(jobId: String): JobExecutionConfig {
    val json = restClient.performGetRequest("/jobs/$jobId/config")
    return BdtJson.fromJsonToClass(json, JobConfiguration::class.java).executionConfig
  }

  fun getTaskManagers(): List<TaskManagerInfo> {
    val json = restClient.performGetRequest("/taskmanagers")
    return BdtJson.fromJsonToClass(json, TaskManagers::class.java).taskmanagers
  }

  fun getTaskManagerLog(taskManagerId: String): String {
    return restClient.performGetRequest("/taskmanagers/$taskManagerId/log")
  }

  fun getTaskManagerStdOut(taskManagerId: String): String {
    return restClient.performGetRequest("/taskmanagers/$taskManagerId/stdout")
  }

  fun getTaskManagerLogList(taskManagerId: String): List<LogFileInfo> {
    val json = restClient.performGetRequest("/taskmanagers/$taskManagerId/logs")
    return BdtJson.fromJsonToClass(json, JobManagerLogs::class.java).logs
  }

  fun getTaskManagerLogFile(taskManagerId: String, uri: String): String {
    return restClient.performGetRequest("/taskmanagers/$taskManagerId/logs/$uri")
  }

  fun getTaskManagerThreadDump(taskManagerId: String): String {
    return restClient.performGetRequest("/taskmanagers/$taskManagerId/thread-dump")
  }

  fun getJobManagerConfig(): List<JobManagerConfig> {
    val json = restClient.performGetRequest("/jobmanager/config")
    return BdtJson.fromJsonArray(json, JobManagerConfig::class.java)
  }

  fun getJobManagerLog(): String {
    return restClient.performGetRequest("/jobmanager/log")
  }

  fun getJobMangerStdOut(): String {
    return restClient.performGetRequest("/jobmanager/stdout")
  }

  fun getJobMangerLogList(): List<LogFileInfo> {
    val json = restClient.performGetRequest("/jobmanager/logs")
    return BdtJson.fromJsonToClass(json, JobManagerLogs::class.java).logs
  }

  fun getJobManagerLogFile(uri: String): String {
    return restClient.performGetRequest("/jobmanager/logs/$uri")
  }
}