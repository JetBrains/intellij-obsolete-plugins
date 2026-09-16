package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class UserMetricsInfo {
  var appsSubmitted = 0
  var appsCompleted = 0
  var appsPending = 0
  var appsRunning = 0
  var appsFailed = 0
  var appsKilled = 0
  var runningContainers = 0
  var pendingContainers = 0
  var reservedContainers = 0
  var reservedMB: Long = 0
  var pendingMB: Long = 0
  var allocatedMB: Long = 0
  var reservedVirtualCores: Long = 0
  var pendingVirtualCores: Long = 0
  var allocatedVirtualCores: Long = 0
}