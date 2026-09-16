package com.intellij.bigdatatools.emr.client

import com.intellij.bigdatatools.awsBase.connection.AwsConnectionUtils
import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.awsBase.connection.auth.AwsAuthUtil
import com.intellij.bigdatatools.awsBase.driver.AwsCredentialController
import com.intellij.bigdatatools.emr.model.EmrClusterInstanceInfo
import com.intellij.bigdatatools.emr.model.EmrClusterState
import com.intellij.bigdatatools.emr.settings.EmrConnectionData
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.common.rfs.driver.runBlockingInterruptible
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.profiles.ProfileFile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.emr.EmrClient
import software.amazon.awssdk.services.emr.model.AddJobFlowStepsRequest
import software.amazon.awssdk.services.emr.model.CancelStepsRequest
import software.amazon.awssdk.services.emr.model.ClusterSummary
import software.amazon.awssdk.services.emr.model.DescribeClusterRequest
import software.amazon.awssdk.services.emr.model.DescribeClusterResponse
import software.amazon.awssdk.services.emr.model.DescribeStepRequest
import software.amazon.awssdk.services.emr.model.Instance
import software.amazon.awssdk.services.emr.model.InstanceGroupType
import software.amazon.awssdk.services.emr.model.InstanceState
import software.amazon.awssdk.services.emr.model.ListClustersRequest
import software.amazon.awssdk.services.emr.model.ListInstancesRequest
import software.amazon.awssdk.services.emr.model.ListStepsRequest
import software.amazon.awssdk.services.emr.model.SetTerminationProtectionRequest
import software.amazon.awssdk.services.emr.model.Step
import software.amazon.awssdk.services.emr.model.StepConfig
import software.amazon.awssdk.services.emr.model.StepState
import software.amazon.awssdk.services.emr.model.StepSummary
import software.amazon.awssdk.services.emr.model.TerminateJobFlowsRequest

class BdtEmrClient(project: Project?, private val connData: EmrConnectionData) : MonitoringClient(project) {
  private val authentication = AwsAuthUtil.getPrimaryAuthentication(connData.getAwsInfo())
  private val credentialsController = AwsCredentialController(authentication.getCredentialsProvider())

  private var client: EmrClient? = null

  override fun getRealUri(): String = "https://console.aws.amazon.com/elasticmapreduce/home?region=${connData.region}"

  override fun connectInner(calledByUser: Boolean) {
    credentialsController.wrapWithAllowDialogs(calledByUser) {
      if (client == null || calledByUser) {
        client?.close()
        client = createClient()
      }

      getClusters(limit = 1)
    }
  }

  override fun checkConnectionInner() {
    getClusters(limit = 1)
  }

  override fun dispose() {
    try {
      client?.close()
    }
    catch (t: Throwable) {
      thisLogger().warn(t)
    }
  }

  fun getSteps(clusterId: String, states: List<StepState>? = null, filter: String? = null, limit: Int? = 100): List<StepSummary> {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    var request = ListStepsRequest.builder().clusterId(clusterId)
    if (states != null) {
      request = request.stepStates(*states.toTypedArray())
    }

    var left = limit
    val filterRegex = filter?.let { Regex(it) }
    val totalResult = mutableListOf<StepSummary>()
    while (true) {
      if (left == 0)
        return totalResult

      val response = client.listSteps(request.build())
      val steps = response.steps().let {
        if (filterRegex == null)
          it
        else
          it.filter { stepSummary ->
            val stepString = stepSummary.toString()
            stepString.contains(filterRegex)
          }
      }
      if (left == null) {
        totalResult.addAll(steps)
      }
      if (left != null && steps.size >= left) {
        totalResult.addAll(steps.subList(0, left))
        return totalResult
      }
      if (left != null && steps.size < left) {
        totalResult.addAll(steps)
        left -= steps.size
      }
      val nextMarker = response.marker()
      if (nextMarker == null)
        return totalResult
      request = request.marker(nextMarker)
    }
  }

  fun getClusters(states: List<EmrClusterState>? = null, filter: String? = null, limit: Int?): List<ClusterSummary> {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    var request = ListClustersRequest.builder()
    val clusterStates = states?.ifEmpty { null }
    val originStates = clusterStates?.flatMap { it.toOriginStates() }?.distinct()?.ifEmpty { null }
    if (originStates != null) {
      request = request.clusterStates(*originStates.toTypedArray())
    }

    var left = limit
    val filterRegex = filter?.let { Regex(it) }
    val totalResult = mutableListOf<ClusterSummary>()
    while (true) {
      if (left == 0)
        return totalResult

      val response = runBlockingInterruptible {
        client.listClusters(request.build())
      }
      val rawClusters = response.clusters()
      val clustersFilteredByState = if (clusterStates != null) {
        rawClusters.filter { cluster -> clusterStates.any { it.isSupported(cluster) } }
      }
      else {
        rawClusters
      }

      val clusters = clustersFilteredByState.let {
        if (filterRegex == null)
          it
        else
          it.filter { clusterSummary ->
            val clusterString = clusterSummary.name() + clusterSummary.id() + clusterSummary.status().stateAsString()
            clusterString.contains(filterRegex)
          }
      }
      if (left == null) {
        totalResult.addAll(clusters)
      }
      if (left != null && clusters.size >= left) {
        totalResult.addAll(clusters.subList(0, left))
        return totalResult
      }
      if (left != null && clusters.size < left) {
        totalResult.addAll(clusters)
        left -= clusters.size
      }
      val nextMarker = response.marker()
      if (nextMarker == null)
        return totalResult
      request = request.marker(nextMarker)
    }
  }

  fun getClusterInstances(id: String,
                          filteredTypes: List<InstanceGroupType>? = null,
                          states: List<InstanceState>? = null,
                          filter: String? = null,
                          limit: Int? = 1): List<EmrClusterInstanceInfo> {
    val types = filteredTypes ?: InstanceGroupType.knownValues()

    var totalLimit = limit
    val results = mutableListOf<EmrClusterInstanceInfo>()
    InstanceGroupType.knownValues().forEach { groupType ->
      if (groupType !in types)
        return@forEach
      val res = getClusterInstancesByTypes(id, groupType, states, filter, totalLimit)
      if (totalLimit != null) {
        totalLimit -= res.size
      }
      results.addAll(res.map { EmrClusterInstanceInfo.getFrom(it, groupType) })
      if (totalLimit == 0)
        return results
    }
    return results
  }

  private fun getClusterInstancesByTypes(id: String,
                                         type: InstanceGroupType,
                                         states: List<InstanceState>? = null,
                                         filter: String? = null,
                                         limit: Int? = 1): List<Instance> {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    var request = ListInstancesRequest.builder().clusterId(id).instanceGroupTypes(type)
    if (states != null) {
      request = request.instanceStates(*states.toTypedArray())
    }

    var left = limit
    val filterRegex = filter?.let { Regex(it) }
    val totalResult = mutableListOf<Instance>()
    while (true) {
      if (left == 0)
        return totalResult

      val response = client.listInstances(request.build())
      val instances = response.instances().let {
        if (filterRegex == null)
          it
        else
          it.filter { instanceInfo ->
            val clusterString = instanceInfo.toString()
            clusterString.contains(filterRegex)
          }
      }
      if (left == null) {
        totalResult.addAll(instances)
      }
      if (left != null && instances.size >= left) {
        totalResult.addAll(instances.subList(0, left))
        return totalResult
      }
      if (left != null && instances.size < left) {
        totalResult.addAll(instances)
        left -= instances.size
      }
      val nextMarker = response.marker()
      if (nextMarker == null)
        return totalResult
      request = request.marker(nextMarker)
    }
  }

  fun getClusterDetails(clusterId: String): DescribeClusterResponse {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    val describeClusterRequest = DescribeClusterRequest.builder().clusterId(clusterId)
    return client.describeCluster(describeClusterRequest.build())
  }

  fun setTerminationProtection(clusterId: String, enable: Boolean) {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    val describeClusterRequest = SetTerminationProtectionRequest.builder()
      .jobFlowIds(listOf(clusterId))
      .terminationProtected(enable)
    client.setTerminationProtection(describeClusterRequest.build())
  }

  fun addSteps(clusterId: String, stepConfig: StepConfig): List<String> {
    val client = client ?: error(NOT_INIT_ERROR_MSG)

    val request = AddJobFlowStepsRequest.builder()
      .jobFlowId(clusterId)
      .steps(stepConfig)
    return client.addJobFlowSteps(request.build()).stepIds() ?: emptyList()
  }


  fun terminateCluster(id: String) {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    val request = TerminateJobFlowsRequest.builder().jobFlowIds(id)
    client.terminateJobFlows(request.build())
  }

  fun cancelSteps(clusterId: String, stepIds: List<String>) {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    val request = CancelStepsRequest.builder().clusterId(clusterId).stepIds(stepIds)
    client.cancelSteps(request.build())
  }

  fun getStepDetails(clusterId: String, stepId: String): Step? = try {
    val client = client ?: error(NOT_INIT_ERROR_MSG)
    val request = DescribeStepRequest.builder().clusterId(clusterId).stepId(stepId)
    client.describeStep(request.build()).step()
  }
  catch (t: Throwable) {
    thisLogger().warn("Cannot get step details of ${clusterId}:${stepId}.", t)
    null
  }

  fun checkIsStepFinished(clusterId: String, stepId: String): Boolean {
    val stepStatus = getStepDetails(clusterId, stepId)?.status()?.state()
    return stepStatus !in setOf(StepState.PENDING, StepState.RUNNING, StepState.CANCEL_PENDING)

  }


  private fun createClient(): EmrClient? {
    val httpClient = AwsConnectionUtils.createHttpClient(connData.getProxy(), connData.trustAllSsl == true)
    val overrideConfiguration = if (connData.activeAuthenticationType in setOf(AuthenticationType.KEY_PAIR.id, AuthenticationType.ANON.id,
                                                                               AuthenticationType.PROFILE_FROM_CREDENTIALS_FILE.id)) {
      val clientConfiguration = ClientOverrideConfiguration.builder()
      clientConfiguration.defaultProfileFile(ProfileFile.aggregator().build()).build()
    }
    else {
      null
    }

    val clientBuilder = EmrClient.builder()
      .credentialsProvider(credentialsController.credentials)
      .region(Region.of(connData.region))
      .httpClient(httpClient)
    if (overrideConfiguration != null)
      clientBuilder.overrideConfiguration(overrideConfiguration)
    return clientBuilder.build()
  }


  companion object {
    private val NOT_INIT_ERROR_MSG = HdfsMessagesBundle.message("emr.is.not.inited")
  }
}