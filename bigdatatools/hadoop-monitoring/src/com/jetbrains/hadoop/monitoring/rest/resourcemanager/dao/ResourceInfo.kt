package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

data class ResourceInfo(
  var memory: Long = 0,
  var vCores: Int = 0
)
