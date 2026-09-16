package com.intellij.bigdatatools.emr.settings

class EmrConfig {
  var selectedCluster: String? = null

  var clusterFilter: String? = null
  var clusterLimit: Int? = 100

  var stepFilter: String? = null
  var stepLimit: Int? = 100

  var instanceFilter: String? = null
  var instanceLimit: Int? = 100
}