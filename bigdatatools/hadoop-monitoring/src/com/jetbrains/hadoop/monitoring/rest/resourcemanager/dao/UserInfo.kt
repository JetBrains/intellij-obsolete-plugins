package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class UserInfo {
  var username: String? = null
  var resourcesUsed: ResourceInfo? = null
  var numPendingApplications = 0
  var numActiveApplications = 0
  var AMResourceUsed: ResourceInfo? = null
  var userResourceLimit: ResourceInfo? = null
  var resources: ResourcesInfo? = null
  var userWeight = 0f
  var isActive = false
}