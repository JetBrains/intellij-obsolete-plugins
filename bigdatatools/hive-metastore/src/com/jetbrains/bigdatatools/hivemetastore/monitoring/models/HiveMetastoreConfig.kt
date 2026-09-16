package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

class HiveMetastoreConfig {
  var databasePattern: String? = null
  var tablePattern: String? = null

  var partitionLimit: Int? = null
}