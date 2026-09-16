package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class CredentialsInfo {
  var tokens: Map<String, String> = emptyMap()
  var secrets: Map<String, String> = emptyMap()
}