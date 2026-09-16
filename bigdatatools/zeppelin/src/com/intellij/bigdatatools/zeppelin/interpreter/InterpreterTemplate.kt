package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterProperty
import com.intellij.openapi.util.NlsSafe

data class InterpreterTemplate(
  @NlsSafe val group: String,
  val properties: List<InterpreterProperty>
)