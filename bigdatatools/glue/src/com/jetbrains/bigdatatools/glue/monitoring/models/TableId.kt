package com.jetbrains.bigdatatools.glue.monitoring.models

import com.jetbrains.bigdatatools.glue.client.GlueDataManager

data class TableId(val catalog: String, val database: String, val table: String) {
  override fun toString() = "$catalog${GlueDataManager.DELIMITER}$database${GlueDataManager.DELIMITER}$table"

  companion object {
    fun fromString(s: String): TableId {
      val (catalog, database, table) = s.split(GlueDataManager.DELIMITER)
      return TableId(catalog, database, table)
    }
  }
}