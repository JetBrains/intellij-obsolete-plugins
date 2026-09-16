package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/*
 * DAO object to display application allocation detailed information.
 */
class AppAllocationInfo {
  var nodeId: String? = null
  var queueName: String? = null
  var appPriority: String? = null
  var allocatedContainerId: String? = null
  var allocationState: String? = null
  var diagnostic: String? = null
  var timeStamp: String? = null
  var allocationAttempt: List<ActivityNodeInfo> = emptyList()
}