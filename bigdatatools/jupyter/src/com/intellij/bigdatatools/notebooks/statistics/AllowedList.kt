package com.intellij.bigdatatools.notebooks.statistics

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell

// This object is extracted from Zeppelin module, because cellCode white list and method for getting interpreter are shared between
object AllowedList {
  const val UNKNOWN = "unknown"
  val cellCodeAllowedList = listOf("default", "alluxio", "angular", "bigquery", "cassandra", "elasticsearch", "file", "flink",
                                   "groovy", "hbase", "ignite", "ignitesql", "jdbc", "kylin", "lens", "livy", "pyspark", "pyspark3",
                                   "sparkr", "shared", "md", "neo4j", "pig", "query", "python", "ipython", "sql", "conda", "docker",
                                   "sap", "sh", "spark", "dep", "ipyspark", "r", "athena", UNKNOWN)

  fun cellInterpreterCode(cell: NotebookCell): String {
    val lastPartOfInterpreterCode = cell.interpreterCode.split(".").last()
    if (lastPartOfInterpreterCode.isEmpty()) return "default"
    return if (cellCodeAllowedList.contains(lastPartOfInterpreterCode))
      lastPartOfInterpreterCode
    else
      UNKNOWN
  }
}