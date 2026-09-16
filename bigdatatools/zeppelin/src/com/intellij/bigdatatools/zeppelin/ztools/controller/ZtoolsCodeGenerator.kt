package com.intellij.bigdatatools.zeppelin.ztools.controller

import com.intellij.bigdatatools.zeppelin.ztools.collector.ZtoolsRefSqlTableInfo

object ZtoolsCodeGenerator {
  const val SCALA_ZTOOLS_WARNING_HEADER = "// It is generated code for integration with Big Data Tools plugin\n" +
                                          "// Please DO NOT edit it.\n"
  const val PYTHON_ZTOOLS_WARNING_HEADER = "# It is generated code for integration with Big Data Tools plugin\n" +
                                           "# Please DO NOT edit it.\n"

  private val collectSqlSparkCode by lazy {
    this.javaClass.getResourceAsStream("/template/ztools-spark-database.scala")?.bufferedReader()?.readText()
    ?: error("Template is not found for StateViewer Collect SQL Schema")
  }

  private val collectSqlPysparkCode by lazy {
    this.javaClass.getResourceAsStream("/template/ztools-pyspark-database.py")?.bufferedReader()?.readText()
    ?: error("Template is not found for StateViewer Collect SQL Schema")
  }


  private val collectInterpreterValuesScala by lazy {
    this.javaClass.getResourceAsStream("/template/ztools-spark-scala.scala")?.bufferedReader()?.readText()
    ?: error("Template is not found for StateViewer Collect Scala Code")
  }

  private val collectInterpreterValuesPython by lazy {
    this.javaClass.getResourceAsStream("/template/ztools-spark-python.py")?.bufferedReader()?.readText()
    ?: error("Template is not found for StateViewer Collect PySpark Code")
  }

  fun getCollectPythonDataframes(
    depth: Int,
    collectionSizeLimit: Int,
    stringSizeLimit: Int,
    timeout: Int,
    filterNames: List<String>?,
  ): String {
    val filterString = filterNames?.joinToString("\", \"")?.let { "[\"$it\"]" } ?: "None"
    return collectInterpreterValuesPython.format(depth, stringSizeLimit, collectionSizeLimit, timeout, filterString)
  }

  fun getCollectScalaDataframes(
    depth: Int,
    enableProfiling: Boolean,
    collectionSizeLimit: Int,
    stringSizeLimit: Int,
    timeout: Int,
    variableTimeout: Int,
    interpreterResCountLimit: Int,
    filterNames: List<String>?,
  ): String {
    val filterString = filterNames?.joinToString("\", \"")?.let { "List(\"$it\")" } ?: "null"
    return collectInterpreterValuesScala.format(depth.toString(),
                                                enableProfiling.toString(),
                                                collectionSizeLimit.toString(),
                                                stringSizeLimit.toString(),
                                                timeout.toString(),
                                                variableTimeout.toString(),
                                                interpreterResCountLimit.toString(),
                                                filterString)
  }

  fun getCollectSqlSpark(sqlTableCollectSqls: List<String>?,
                         noteTableNames: List<ZtoolsRefSqlTableInfo>?,
                         timeout: Long,
                         collectOnlyTempTables: Boolean,
                         appendOutput: Boolean): String {
    val sqlTablesShow = sqlTableCollectSqls?.joinToString(prefix = "Array(", postfix = ")", separator = ",") {
      "\"" + it.replace("\"", "\\\"") + "\""
    } ?: "null"

    val forcedTables = noteTableNames?.joinToString(separator = ", ") {
      val database = it.database.replace("\"", "\\\"")
      val table = it.table.replace("\"", "\\\"")
      "(\"$database\", \"$table\")"
    } ?: ""


    return collectSqlSparkCode.format(sqlTablesShow,
                                      forcedTables,
                                      timeout,
                                      collectOnlyTempTables,
                                      appendOutput)
  }

  fun getCollectSqlPyspark(sqlTableCollectSqls: List<String>?,
                           noteTableNames: List<ZtoolsRefSqlTableInfo>?,
                           timeout: Long,
                           collectOnlyTempTables: Boolean,
                           appendOutput: Boolean): String {
    val sqlTablesShow = sqlTableCollectSqls?.joinToString(separator = ",", prefix = "[", postfix = "]") {
      "\"" + it.replace("\"", "\\\"") + "\""
    } ?: "None"

    val forcedTables = noteTableNames?.joinToString(separator = ", ") {
      val database = it.database.replace("\"", "\\\"")
      val table = it.table.replace("\"", "\\\"")
      "(\"$database\", \"$table\")"
    } ?: ""


    return collectSqlPysparkCode.format(
      sqlTablesShow,
      forcedTables,
      timeout,
      if (collectOnlyTempTables) "True" else "False",
      if (appendOutput) "True" else "False",
    )
  }
}