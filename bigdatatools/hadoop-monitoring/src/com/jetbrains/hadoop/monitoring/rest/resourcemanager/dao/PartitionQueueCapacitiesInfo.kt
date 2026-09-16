package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * This class represents queue capacities for a given partition
 */
class PartitionQueueCapacitiesInfo {
  var partitionName: String? = null
  var capacity = 0f
  var usedCapacity = 0f
  var maxCapacity = 100f
  var absoluteCapacity = 0f
  var absoluteUsedCapacity = 0f
  var absoluteMaxCapacity = 100f
  var maxAMLimitPercentage = 0f
  var configuredMinResource: ResourceInfo? = null
  var configuredMaxResource: ResourceInfo? = null
  var effectiveMinResource: ResourceInfo? = null
  var effectiveMaxResource: ResourceInfo? = null
}