package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * DAO which wraps PartitionQueueCapacitiesInfo applicable for a queue
 */
class QueueCapacitiesInfo {
  var queueCapacitiesByPartition: List<PartitionQueueCapacitiesInfo> = emptyList()
}