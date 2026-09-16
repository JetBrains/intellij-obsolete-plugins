package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class FifoSchedulerInfo : SchedulerInfo() {
  var capacity = 0f
  var usedCapacity = 0f
  var qstate: QueueState? = null
  var minQueueMemoryCapacity: Long = 0
  var maxQueueMemoryCapacity: Long = 0
  var numNodes = 0
  var usedNodeCapacity = 0
  var availNodeCapacity = 0
  var totalNodeCapacity = 0
  var numContainers = 0
}