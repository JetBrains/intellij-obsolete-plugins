package com.jetbrains.bigdatatools.hivemetastore.monitoring.models

import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager

data class TableId(val catalog: String, val database: String, val table: String) {
  override fun toString() = "$catalog${HiveDataManager.DELIMITER}$database${HiveDataManager.DELIMITER}$table"

  companion object {
    fun fromString(s: String): TableId {
      val (catalog, database, table) = s.split(HiveDataManager.DELIMITER)
      return TableId(catalog, database, table)
    }
  }
}