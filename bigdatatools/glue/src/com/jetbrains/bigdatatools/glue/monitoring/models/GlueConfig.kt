package com.jetbrains.bigdatatools.glue.monitoring.models

class GlueConfig {
  var databaseResourceShareType: String? = null

  var tablePattern: String? = null
  var tableLimit: Int? = null

  var partitionLimit: Int? = null
}