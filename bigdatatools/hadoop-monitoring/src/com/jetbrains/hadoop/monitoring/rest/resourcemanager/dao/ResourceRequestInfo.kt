package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class representing a resource request.
 */
class ResourceRequestInfo {
  var priority: AppPriority? = null
  var allocationRequestId: Long = 0
  var resourceName: String? = null
  var capability: ResourceInfo? = null
  var numContainers = 0
  var relaxLocality = false
  var nodeLabelExpression: String? = null
  var executionTypeRequest: ExecutionTypeRequestInfo? = null
  var placementConstraint: String? = null
  var allocationTags: Set<String>? = null
}