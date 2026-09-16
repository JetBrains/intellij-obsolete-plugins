package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager

data class DatabaseId(val catalog: String, val database: String) {
  override fun toString() = "$catalog${HiveDataManager.DELIMITER}$database"

  companion object {
    fun fromString(s: String): DatabaseId {
      val (catalog, database) = s.split(HiveDataManager.DELIMITER)
      return DatabaseId(catalog, database)
    }
  }
}