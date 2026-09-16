package com.intellij.bigdatatools.zeppelin.ztools.settings

data class ZtoolsConfig(
  val profiling: Boolean = false,
  val variablesSettings: ZtoolsVariablesSettings = ZtoolsVariablesSettings(),
  val sqlSettings: ZtoolsSqlSettings = ZtoolsSqlSettings()
)
