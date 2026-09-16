package com.intellij.bigdatatools.zeppelin.ztools.collector

object ZtoolsCollectorHelpers {
  private val CREATE_GLOBAL_TEMP_TABLE_COMMANDS = setOf("createGlobalTempView", "createOrReplaceGlobalTempView")
  private val CREATE_TEMP_TABLE_COMMANDS = setOf("createTempView", "createOrReplaceTempView", "registerTempTable")
  private val CREATE_TABLE_COMMANDS = CREATE_GLOBAL_TEMP_TABLE_COMMANDS + CREATE_TEMP_TABLE_COMMANDS

  fun isCreateTableMethod(methodName: String?) =  methodName in CREATE_TABLE_COMMANDS

  fun getTableInfo(methodName:String, dataFrameName: String): ZtoolsRefSqlTableInfo = if (methodName in CREATE_TEMP_TABLE_COMMANDS) {
    ZtoolsRefSqlTableInfo("",dataFrameName)
  } else {
    ZtoolsRefSqlTableInfo("global_temp", dataFrameName)
  }
}