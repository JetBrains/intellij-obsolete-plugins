package com.intellij.bigdatatools.zeppelin.models

open class ZeppelinException(val msg: String? = null) : Exception() {
  override val message: String
    get() = "Error in Zeppelin plugin. ${msg ?: ""}"
}


data class NotebookNotFoundException(val id: String) : ZeppelinException() {
  override val message: String
    get() = "Notebook with $id id is not found."
}


class ParseException(private val json: String,
                     private val parseClass: String) : ZeppelinException() {
  override val message: String
    get() = "Cannot parse json to object.\nJson:\n$json\nClass:\n$parseClass"
}