package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class representing a reservation request.
 */
class ReservationRequestInfo {
  var capability: ResourceInfo? = null
  var minConcurrency = 0
  var numContainers = 0
  var duration: Long = 0
}