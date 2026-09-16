package com.intellij.bigdatatools.zeppelin.ztools.settings

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle

data class ZtoolsSqlSettings(
  val isEnabled: Boolean = true,
  val timeout: Long = 5_000,

  val collectionStrategy: ZtoolsSqlCollectStrategy = ZtoolsSqlCollectStrategy.DEFINED_ON_EACH_RUN_ALL_REFRESH,
  val tableFilters: List<ZtoolFilterRow> = listOf(ZtoolFilterRow("default", "*")),
  val collectOnlyTempTables: Boolean = false,
)


data class ZtoolFilterRow(var database: String, var tablePattern: String)

enum class ZtoolsSqlCollectStrategy(val desc: String) {
  ALL_ON_EACH_RUN(ZepMessagesBundle.message("ztools.sql.settings.collect.type.allOnEachRun")),
  DEFINED_ON_EACH_RUN_ALL_REFRESH(ZepMessagesBundle.message("ztools.sql.settings.collect.type.definedOnRunAllOnRefresh")),
  ONLY_ON_REFRESH(ZepMessagesBundle.message("ztools.sql.settings.collect.type.onlyOnRefresh"))
}