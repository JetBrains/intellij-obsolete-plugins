package com.intellij.bigdatatools.zeppelin.ztools.database.models

import com.jetbrains.bigdatatools.common.database.BdtDbColumn
import com.jetbrains.bigdatatools.common.database.BdtDbTable

class ZtoolsColumn(val name: String = "", val columnType: String = "", val description: String?)

class ZtoolsTable(val name: String?,
                  val databaseName: String?,
                  val error: String? = null,
                  val columns: List<ZtoolsColumn> = emptyList()) {
  fun toDbTable() = BdtDbTable(name = name ?: "<empty>",
                               databaseName = databaseName?.ifBlank { null } ?: "default",
                               columns = columns.filter { !it.name.isBlank() }
                                 .map { BdtDbColumn(it.name, it.columnType, it.description) })
}

data class ZtoolsSqlProfile(val request: String, val time: Long)

class ZtoolsSqlInfo(
  val tables: List<ZtoolsTable>,
  @Suppress("unused")
  val errors: List<String>,
  @Suppress("unused")
  val profiling: List<ZtoolsSqlProfile>,
  val appendOutput: Boolean)
