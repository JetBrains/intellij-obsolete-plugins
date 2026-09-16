package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao


class NodeToLabelsEntry {
  var nodeId: String? = null
  var labels: List<String> = emptyList()
}