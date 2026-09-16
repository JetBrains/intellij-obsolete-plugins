package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class NodeIDsInfo {
  /**
   * Set doesn't support default no arg constructor which is req by JAXB
   */
  var nodeIDsList: List<String> = listOf()
}