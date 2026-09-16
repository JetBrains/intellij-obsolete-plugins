package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao


class CapacitySchedulerHealthInfo {
  class OperationInformation {
    var operation: String? = null
    var nodeId: String? = null
    var containerId: String? = null
    var queue: String? = null
  }

  class LastRunDetails {
    var operation: String? = null
    var count: Long = 0
    var resources: ResourceInfo? = null
  }

  var lastrun: Long = 0
  //var operationsInfo: List<OperationInformation>? = null
  var lastRunDetails: List<LastRunDetails>? = null
}