package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class ConfInfo {
  var property: List<ConfItem> = emptyList()

  class ConfItem {
    var name: String? = null
    var value: String? = null
  }
}