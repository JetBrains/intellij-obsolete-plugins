package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.notebooks.statistics.AllowedList
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import java.util.Locale

internal object ZeppelinAllowedList {
  val jdbcAllowedList = listOf("postgres", "mysql", "mariadb", "redshift", "hive", "phoenix", "tajo")

  val interpreterGroupAllowedList: List<String> = listOf("spark", "md", "angular", "sh", "livy", "alluxio", "file", "flink",
                                                         "python", "ignite", "lens", "cassandra", "kylin", "elasticsearch", "jdbc",
                                                         "hbase", "bigquery", "pig", "groovy", "neo4j", "sap", "athena",
                                                         AllowedList.UNKNOWN)

  fun interpreterGroup(interpreterGroup: String) =
    interpreterGroupAllowedList.firstOrNull { it == interpreterGroup } ?: AllowedList.UNKNOWN

  fun jdbcDriver(driver: String) =
    jdbcAllowedList.firstOrNull { driver.lowercase(Locale.UK).contains(it) } ?: AllowedList.UNKNOWN

  fun bindingInterpreterId(defaultBinding: Interpreter): String = defaultBinding.interpreters.firstOrNull {
    interpreterGroup(it.name) != AllowedList.UNKNOWN
  }?.name ?: AllowedList.UNKNOWN

  enum class ChangedFieldType {
    TEXT,
    META,
    UNKNOWN
  }

  fun libraryVersion(raw: String): String {
    val matchResult = Regex("[0-9]{1,2}[.][0-9]{1,2}([.][0-9]{1,2})?").find(raw) ?: return AllowedList.UNKNOWN
    return matchResult.value
  }
}