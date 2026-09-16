package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class CapacitySchedulerInfo : SchedulerInfo() {
  var capacity = 0f
  var usedCapacity = 0f
  var maxCapacity = 0f
  var queueName: String? = null
  var queues: CapacitySchedulerQueueInfoList? = null
  var capacities: QueueCapacitiesInfo? = null
  var health: CapacitySchedulerHealthInfo? = null
}