package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class PySparkSchemaInfo private constructor(val isPartial: Boolean,
                                            val schemaColumns: List<@NlsSafe String>, private val groupByColumns: List<String>? = null) {

  fun withColumn(column: String): PySparkSchemaInfo {
    return if (column in schemaColumns)
      this
    else
      create(isPartial, schemaColumns + column)
  }

  fun withColumns(columns: List<String>): PySparkSchemaInfo {
    val newColumns = columns - schemaColumns.toSet()
    return create(isPartial, schemaColumns + newColumns)
  }

  fun select(columns: Collection<String>): PySparkSchemaInfo {
    return if ("*" in columns)
      PySparkSchemaInfo(isPartial, schemaColumns)
    else
      PySparkSchemaInfo(false, columns.toList())
  }

  fun drop(columns: Collection<String>) = PySparkSchemaInfo(isPartial, schemaColumns - columns.toSet())

  fun renameColumn(oldColName: String, newColName: String): PySparkSchemaInfo {
    return PySparkSchemaInfo(isPartial, schemaColumns - oldColName + newColName)
  }

  @NlsSafe
  fun toPresentable(): @NlsSafe String {
    val cols = schemaColumns.joinToString(separator = ", ")
    return SparkMessagesBundle.message("pyspark.documentation.dataframe.schema", cols)
  }

  @NlsSafe
  override fun toString() = schemaColumns.joinToString { it }

  @Suppress("unused")
  fun getColumnByRegex(regexString: String?): String? {
    regexString ?: return null

    val regex = Regex(regexString.removePrefix("`").removeSuffix("`"))
    return schemaColumns.firstOrNull { regex.matches(it) }
  }

  fun crossJoin(info: PySparkSchemaInfo): PySparkSchemaInfo {
    return PySparkSchemaInfo(isPartial || info.isPartial, (info.schemaColumns + schemaColumns).distinct())
  }

  fun join(otherDf: PySparkSchemaInfo): PySparkSchemaInfo {
    return PySparkSchemaInfo(isPartial || otherDf.isPartial, schemaColumns + otherDf.schemaColumns)
  }

  fun groupBy(args: List<String>): PySparkSchemaInfo {
    return PySparkSchemaInfo(isPartial, schemaColumns, groupByColumns = args)
  }

  fun finishGroupingColumn(groupColumn: String?): PySparkSchemaInfo = finishGrouping(listOfNotNull(groupColumn))

  fun finishGrouping(groupColumn: List<String>): PySparkSchemaInfo {
    val columns = groupByColumns ?: emptyList()
    return create(isPartial, columns + groupColumn)
  }

  fun unionByName(otherDf: PySparkSchemaInfo?): PySparkSchemaInfo {
    if (otherDf == null)
      return this
    val newColumns = schemaColumns + (otherDf.schemaColumns - schemaColumns.toSet())
    return create(isPartial || otherDf.isPartial, newColumns)
  }

  fun unpivot(sourceColumns: List<String>, newColumn1: String, newColumn2: String): PySparkSchemaInfo {
    return PySparkSchemaInfo(isPartial, sourceColumns + newColumn1 + newColumn2)
  }

  fun withColumnsRenamed(renameKeys: List<Pair<String, String>>): PySparkSchemaInfo {
    val columns = schemaColumns.toMutableList()
    renameKeys.forEach {
      columns -= it.first
      if (it.second !in columns)
        columns += it.second
    }
    return create(isPartial, columns)
  }

  companion object {
    fun create(isPartial: Boolean, columns: List<String>): PySparkSchemaInfo {
      return PySparkSchemaInfo(isPartial, columns)
    }

    fun createNotRecognized() = PySparkSchemaInfo(true, emptyList())

    fun createFromSchemaPart(schemaParts: DfTypeSchema): PySparkSchemaInfo {
      val columns = schemaParts.map.map { it.key } + schemaParts.aliases.map { it.key }
      return PySparkSchemaInfo(isPartial = false, columns)
    }
  }
}