package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class to allow users to send information required to create a
 * ContainerLaunchContext which can then be used as part of the
 * ApplicationSubmissionContext
 *
 */
class LogAggregationContextInfo {
  var logIncludePattern: String? = null
  var logExcludePattern: String? = null
  var rolledLogsIncludePattern: String? = null
  var rolledLogsExcludePattern: String? = null
  var policyClassName: String? = null
  var policyParameters: String? = null
}