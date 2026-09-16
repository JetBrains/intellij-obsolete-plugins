package com.jetbrains.bigdatatools.glue.monitoring.models

import com.jetbrains.bigdatatools.glue.client.GlueDataManager

data class DatabaseId(val catalog: String, val database: String) {
  override fun toString() = "$catalog${GlueDataManager.DELIMITER}$database"

  companion object {
    fun fromString(s: String): DatabaseId {
      val (catalog, database) = s.split(GlueDataManager.DELIMITER)
      return DatabaseId(catalog, database)
    }
  }
}