package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * This class represents queue/user resource usage info for a given partition
 */
class PartitionResourcesInfo {
  var partitionName: String? = null
  var used = ResourceInfo()
  var reserved: ResourceInfo? = null
  var pending: ResourceInfo? = null
  var amUsed: ResourceInfo? = null
  var amLimit = ResourceInfo()
  var userAmLimit: ResourceInfo? = null
}