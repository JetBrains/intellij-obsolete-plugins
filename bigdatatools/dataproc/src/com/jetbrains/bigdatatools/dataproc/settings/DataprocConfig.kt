package com.jetbrains.bigdatatools.dataproc.settings

class DataprocConfig {
  var selectedCluster: String? = null

  var textFilter: String? = null
  var clusterLimit: Int? = 100

  var jobFilter: String? = null
  var jobLimit: Int? = 100
}