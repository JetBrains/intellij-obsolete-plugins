package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import java.net.URI

class LocalResourceInfo {
  var url: URI? = null
  var type: LocalResourceType? = null
  var visibility: LocalResourceVisibility? = null
  var size: Long = 0
  var timestamp: Long = 0
  var pattern: String? = null
}