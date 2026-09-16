package com.intellij.bigdatatools.zeppelin.ztools.settings

data class ZtoolsVariablesSettings(
  val isEnabled: Boolean = true,

  val isOnDemandOnly: Boolean = false,
  val timeout: Int = 5_000,

  val sameNoteOnly: Boolean = false,

  val depth: Int = 2,
  val collectionSizeLimit: Int = 100,
  val stringSizeLimit: Int = 400,
  val variableTimeout: Int = 2000,
  val interpreterResCountLimit: Int = 10,
)