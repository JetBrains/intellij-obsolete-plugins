package com.intellij.bigdatatools.zeppelin.ztools.controller.model

import java.util.UUID

@Suppress("DuplicatedCode")
data class ZtoolsDebugTask(
  val interpreterGroup: String,
  val interpreterName: String,
  val language: String,
  val debugId: String = UUID.randomUUID().toString(),
  val subInterpreter: String,
  val onDemand: Boolean,
  val forceIgnoreSql: Boolean) {

  val interpreterCode = "$interpreterName.$subInterpreter"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ZtoolsDebugTask

    if (interpreterName != other.interpreterName) return false
    if (subInterpreter != other.subInterpreter) return false
    if (language != other.language) return false
    if (onDemand != other.onDemand) return false

    return true
  }

  override fun hashCode(): Int {
    var result = interpreterName.hashCode()
    result = 31 * result + subInterpreter.hashCode()
    result = 31 * result + language.hashCode()
    result = 31 * result + onDemand.hashCode()
    return result
  }
}
