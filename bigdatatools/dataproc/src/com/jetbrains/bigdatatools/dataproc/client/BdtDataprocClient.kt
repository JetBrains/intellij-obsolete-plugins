package com.jetbrains.bigdatatools.dataproc.client

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.httpjson.InstantiatingHttpJsonChannelProvider
import com.google.cloud.dataproc.v1.Cluster
import com.google.cloud.dataproc.v1.ClusterControllerClient
import com.google.cloud.dataproc.v1.ClusterControllerSettings
import com.google.cloud.dataproc.v1.DeleteClusterRequest
import com.google.cloud.dataproc.v1.GetClusterRequest
import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.JobControllerClient
import com.google.cloud.dataproc.v1.JobControllerSettings
import com.google.cloud.dataproc.v1.ListClustersRequest
import com.google.cloud.dataproc.v1.ListJobsRequest
import com.google.cloud.dataproc.v1.StartClusterRequest
import com.google.cloud.dataproc.v1.StopClusterRequest
import com.intellij.bigdatatools.coreUi.connection.exception.BdtConfigurationException
import com.intellij.bigdatatools.coreUi.util.withPluginClassLoader
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterState
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo.Companion.jobId
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobState
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobState.Companion.state
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.bigdatatools.gcloud.auth.GcloudAuthFactory
import com.jetbrains.bigdatatools.gcloud.utils.GcloudMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle


class BdtDataprocClient(project: Project?, private val connData: DataprocConnectionData) : MonitoringClient(project) {
  private var clusterControllerClient: ClusterControllerClient? = null
  private var jobControllerClient: JobControllerClient? = null

  override fun getRealUri(): String = "https://console.cloud.google.com/dataproc/"

  override fun connectInner(calledByUser: Boolean) = withPluginClassLoader {
    if (connData.projectId == null) {
      throw BdtConfigurationException(GcloudMessagesBundle.message("error.project.id.is.not.setup"))
    }
    val credentialsProvider = GcloudAuthFactory.createCredentialProvider(project, connData, byUser = calledByUser)

    if (clusterControllerClient == null || calledByUser) {
      clusterControllerClient?.close()
      clusterControllerClient = createClusterClient(credentialsProvider)
    }

    if (jobControllerClient == null || calledByUser) {
      jobControllerClient?.close()
      jobControllerClient = createJobClient(credentialsProvider)
    }
  }

  override fun checkConnectionInner() {
    withPluginClassLoader {
      val jobsRequest = ListJobsRequest.newBuilder().setRegion(connData.region).setProjectId(connData.projectId).setPageSize(1).build()
      val client = jobControllerClient ?: error(NOT_INIT_ERROR_MSG)
      client.listJobs(jobsRequest).page.values
    }
  }

  override fun dispose() {
    clusterControllerClient?.close()
    jobControllerClient?.close()
  }

  fun getJobs(clusterName: String?,
              textFilter: String = "",
              limit: Int? = null,
              supportedStates: List<DataprocJobState> = emptyList()): List<Job> {
    val client = jobControllerClient ?: error(NOT_INIT_ERROR_MSG)

    val requestBuilder = ListJobsRequest.newBuilder()
    requestBuilder.region = connData.region
    requestBuilder.projectId = connData.projectId
    clusterName?.takeIf { it.isNotBlank() }?.let {
      requestBuilder.clusterName = it
    }

    limit?.let { requestBuilder.pageSize = minOf(it, 1000) }

    val response = client.listJobs(requestBuilder.build())
    return response.iterateAll().filter { info ->
      val filterText = textFilter.isBlank() ||
                       info.labelsMap.entries.joinToString { "${it.key}=${it.value}" }.contains(textFilter) ||
                       info.jobId.contains(textFilter)
      val filterState = supportedStates.isEmpty() || supportedStates.any { it.isSupported(info) }

      filterText && filterState
    }.take(limit ?: 5000)

  }

  fun getCluster(clusterName: String): Cluster {
    val client = clusterControllerClient ?: error(NOT_INIT_ERROR_MSG)

    val request = GetClusterRequest.newBuilder()
      .setProjectId(connData.projectId)
      .setRegion(connData.region)
      .setClusterName(clusterName)
      .build()

    return client.getCluster(request)
  }

  fun getClusters(textFilter: String = "",
                  limit: Int? = null,
                  supportedStates: List<DataprocClusterState> = emptyList()): List<Cluster> {
    val client = clusterControllerClient ?: error(NOT_INIT_ERROR_MSG)

    val requestBuilder = ListClustersRequest.newBuilder()
    requestBuilder.region = connData.region
    requestBuilder.projectId = connData.projectId

    limit?.let { requestBuilder.pageSize = minOf(it, 1000) }
    val response = client.listClusters(requestBuilder.build())
    return response.iterateAll().filter { info ->
      val filterText = textFilter.isBlank() ||
                       info.labelsMap.entries.joinToString { "${it.key}=${it.value}" }.contains(textFilter) ||
                       info.clusterName.contains(textFilter)
      val filterState = supportedStates.isEmpty() || supportedStates.any { it.isSupported(info) }

      filterText && filterState
    }.take(limit ?: 5000)
  }


  fun addJob(job: Job): String? {
    val client = jobControllerClient ?: error(NOT_INIT_ERROR_MSG)
    return client.submitJob(connData.projectId, connData.region, job)?.jobId
  }

  fun removeCluster(clusterName: String) {
    val client = clusterControllerClient ?: error(NOT_INIT_ERROR_MSG)
    val request = DeleteClusterRequest.newBuilder()
      .setClusterName(clusterName)
      .setProjectId(connData.projectId!!)
      .setRegion(connData.region)
      .build()
    client.deleteClusterAsync(request).initialFuture.get()
  }


  fun startCluster(clusterName: String) {
    val client = clusterControllerClient ?: error(NOT_INIT_ERROR_MSG)
    val request = StartClusterRequest.newBuilder()
      .setClusterName(clusterName)
      .setProjectId(connData.projectId!!)
      .setRegion(connData.region)
      .build()
    client.startClusterAsync(request).initialFuture.get()
  }


  fun terminateCluster(clusterName: String) {
    val client = clusterControllerClient ?: error(NOT_INIT_ERROR_MSG)
    val stopClusterRequest = StopClusterRequest.newBuilder()
      .setClusterName(clusterName)
      .setProjectId(connData.projectId!!)
      .setRegion(connData.region)
      .build()
    client.stopClusterAsync(stopClusterRequest).initialFuture.get()
  }

  fun deleteJob(jobId: String) {
    val client = jobControllerClient ?: error(NOT_INIT_ERROR_MSG)
    client.deleteJob(connData.projectId, connData.region, jobId)
  }


  fun cancelJob(jobId: String) {
    val client = jobControllerClient ?: error(NOT_INIT_ERROR_MSG)
    client.cancelJob(connData.projectId, connData.region, jobId)
  }

  private fun createClusterClient(credentialsProvider: FixedCredentialsProvider?): ClusterControllerClient {
    val region = connData.region
    val myEndpoint = "${region}-dataproc.googleapis.com:443"

    val provider = InstantiatingHttpJsonChannelProvider.newBuilder().setEndpoint(myEndpoint).build()

    // Configure the settings for the cluster controller client.
    val clusterControllerSettings = ClusterControllerSettings.newBuilder()
      .setTransportChannelProvider(provider)
      .setCredentialsProvider(credentialsProvider)
      .build()
    return ClusterControllerClient.create(clusterControllerSettings)
  }

  private fun createJobClient(credentialsProvider: FixedCredentialsProvider?): JobControllerClient? {
    val region = connData.region
    val myEndpoint = "${region}-dataproc.googleapis.com:443"

    val provider = InstantiatingHttpJsonChannelProvider.newBuilder().setEndpoint(myEndpoint).build()
    // Configure the settings for the cluster controller client.
    val settings = JobControllerSettings.newBuilder()
      .setTransportChannelProvider(provider)
      .setCredentialsProvider(credentialsProvider)
      .build()

    return JobControllerClient.create(settings)
  }

  fun checkIsJobFinished(jobId: String): Boolean {
    val jobInfo = getJobInfo(jobId)
    return jobInfo?.state != DataprocJobState.ACTIVE
  }

  fun getJobInfo(jobId: String) = try {
    jobControllerClient?.getJob(connData.projectId, connData.region, jobId)
  }
  catch (t: Throwable) {
    thisLogger().warn("Cannot get step details of ${jobId}.", t)
    null
  }


  companion object {
    private val NOT_INIT_ERROR_MSG = HdfsMessagesBundle.message("emr.is.not.inited")
  }
}