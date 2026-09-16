package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/*
 * DAO object to display node allocation activity.
 */
class ActivitiesInfo {
  var nodeId: String? = null
  var timeStamp: String? = null
  var diagnostic: String? = null
  var allocations: List<NodeAllocationInfo>? = emptyList()
}