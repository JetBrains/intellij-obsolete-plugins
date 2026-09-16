package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/*
 * DAO object to display application activity.
 */
class AppActivitiesInfo {
  var applicationId: String? = null
  var diagnostic: String? = null
  var timeStamp: String? = null
  var allocations: List<AppAllocationInfo> = emptyList()
}