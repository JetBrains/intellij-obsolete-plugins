package com.intellij.bigdatatools.zeppelin.ztools.database.util

import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsSqlSettings
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableView
import com.jetbrains.bigdatatools.common.database.BdtDbTable

object ZtoolsConverter {
  @Suppress("UNCHECKED_CAST")
  fun convertToVariables(databases: Map<String, List<BdtDbTable>>): MutableMap<String, MutableMap<String, Any>> {
    val resDb = mutableMapOf<String, MutableMap<String, Any>>()
    databases.forEach { db ->
      val dbName = db.key.ifBlank { null } ?: "default"
      db.value.forEach { table ->
        val columnMap = table.columns.associate { column ->
          column.name to mapOf(
            VariableView.KEY to column.name,
            VariableView.TYPE to "Column",
            VariableView.VALUE to column.dataType
          )
        }

        val dbMap = resDb.getOrPut(dbName) {
          mutableMapOf()
        }

        dbMap[VariableView.KEY] = dbName
        dbMap[VariableView.TYPE] = "Database"

        val value: MutableMap<String, Any> = dbMap.getOrPut(VariableView.VALUE) {
          mutableMapOf<String, Any>()
        } as MutableMap<String, Any>


        val tableName = table.name
        value[tableName] = mapOf(
          VariableView.KEY to tableName,
          VariableView.TYPE to "Table",
          VariableView.LENGTH to table.columns,
          VariableView.VALUE to columnMap)
      }
    }

    return resDb
  }

  fun getShowTables(sqlSettings: ZtoolsSqlSettings): List<String> {
    val databaseToTablePatterns = sqlSettings.tableFilters.groupBy { it.database.lowercase() }.map { entry ->
      entry.key.lowercase() to entry.value.map { it.tablePattern }
    }.toMap().toMutableMap()


    return databaseToTablePatterns.map {
      val databaseName = prepareDatabaseName(it.key)

      val fromPart = if (databaseName.isNotBlank()) "FROM $databaseName" else ""

      val tablePatterns = it.value
      val like = if ("*" !in tablePatterns) {
        val regex = tablePatterns.joinToString(separator = "|") { it.trim() }
        "LIKE '$regex'"
      }
      else
        ""

      "SHOW TABLES $fromPart $like"
    }
  }

  fun prepareDatabaseName(databaseName: String) = when (databaseName) {
    "default" -> ""
    "" -> ""
    else -> databaseName
  }

}