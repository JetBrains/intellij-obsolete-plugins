package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class that represent a resource allocation.
 */
class ResourceAllocationInfo {
  var resource: ResourceInfo? = null
  var startTime: Long = 0
  var endTime: Long = 0
}