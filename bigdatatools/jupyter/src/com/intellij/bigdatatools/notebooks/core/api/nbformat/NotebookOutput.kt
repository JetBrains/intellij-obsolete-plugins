package com.intellij.bigdatatools.notebooks.core.api.nbformat

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode

data class NotebookOutput(val code: OutputCode, val msg: List<CellResultMessage>) {
  @Transient
  val json: JsonElement = Gson().toJsonTree(this)
}