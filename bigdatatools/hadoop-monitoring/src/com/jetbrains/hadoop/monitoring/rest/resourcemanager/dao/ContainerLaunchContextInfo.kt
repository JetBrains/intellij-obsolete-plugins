package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class to allow users to send information required to create a
 * ContainerLaunchContext which can then be used as part of the
 * ApplicationSubmissionContext
 *
 */
class ContainerLaunchContextInfo {
  var local_resources: Map<String, LocalResourceInfo>? = null
  var environment: Map<String, String>? = null
  var commands: List<String>? = null
  var servicedata: Map<String, String>? = null
  var credentials: CredentialsInfo? = null
  var acls: Map<ApplicationAccessType, String>? = null
}