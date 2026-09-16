package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/*
 * DAO object to display each node allocation in node heartbeat.
 */
class NodeAllocationInfo {
  var allocatedContainerId: String? = null
  var finalAllocationState: String? = null
  var root: ActivityNodeInfo? = null
}