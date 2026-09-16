package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class to allow users to send information required to create an
 * ApplicationSubmissionContext which can then be used to submit an app
 *
 */
class ApplicationSubmissionContextInfo {
  var applicationId: String? = null
  var applicationName: String? = null
  var queue: String? = null
  var priority = 0
  var containerInfo: ContainerLaunchContextInfo? = null
  var isUnmanagedAM = false
  var cancelTokensWhenComplete = false
  var maxAppAttempts = 0
  var resource: ResourceInfo? = null
  var applicationType: String? = null
  var keepContainers = false
  var tags: Set<String>? = emptySet()
  var appNodeLabelExpression: String? = null
  var amContainerNodeLabelExpression: String? = null
  var logAggregationContextInfo: LogAggregationContextInfo? = null
  var attemptFailuresValidityInterval: Long = 0
  var reservationId: String? = null
}