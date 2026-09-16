package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * DAO which wraps PartitionResourceUsageInfo applicable for a queue/user
 */
class ResourcesInfo {
  var resourceUsagesByPartition: List<PartitionResourcesInfo> = emptyList()
}