package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/*
 * DAO object to display node information in allocation tree.
 * It corresponds to "ActivityNode" class.
 */
class ActivityNodeInfo {
  // The name for activity node
  var name: String? = null
  var appPriority: String? = null
  var requestPriority: String? = null
  var allocationState: String? = null
  var diagnostic: String? = null
  var children: List<ActivityNodeInfo>? = emptyList()
}