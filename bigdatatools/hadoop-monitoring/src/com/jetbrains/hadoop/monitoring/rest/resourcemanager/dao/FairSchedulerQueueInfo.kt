package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

open class FairSchedulerQueueInfo {
  var maxApps = 0
  var minResources: ResourceInfo? = null
  var maxResources: ResourceInfo? = null
  var usedResources: ResourceInfo? = null
  var amUsedResources: ResourceInfo? = null
  var amMaxResources: ResourceInfo? = null
  var demandResources: ResourceInfo? = null
  var steadyFairResources: ResourceInfo? = null
  var fairResources: ResourceInfo? = null
  var clusterResources: ResourceInfo? = null
  var reservedResources: ResourceInfo? = null
  var maxContainerAllocation: ResourceInfo? = null
  var allocatedContainers: Long = 0
  var reservedContainers: Long = 0
  var queueName: String? = null
  var schedulingPolicy: String? = null
  var preemptable = false
  var childQueues: FairSchedulerQueueInfoList? = null
}