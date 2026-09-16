package com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model

import com.squareup.moshi.Json

data class JupyterNote(
  val metadata: Map<String, Any>,
  val nbformat: Int,
  @field:Json(name = "nbformat_minor") @Json(name = "nbformat_minor")
  val nbformat_minor: Int,
  val cells: List<JupyterCell>,
)

data class JupyterCell(
  @field:Json(name = "cell_type") @Json(name = "cell_type")
  val cellType: String,
  val source: Any,
  val metadata: Map<String, Any> = emptyMap(),
  @field:Json(name = "execution_count") @Json(name = "execution_count")
  val executionCount: Int? = null,
  val outputs: List<JupyterCellOutput>? = null,
  val attachments: Map<String, Map<String, String>>? = null
)

data class JupyterCellOutput(
  @field:Json(name = "output_type") @Json(name = "output_type")
  val outputType: String,
  val name: String?,
  val text: Any?,
  val data: Map<String, Any>?,
  val metadata: Map<String, Any>?,
  @field:Json(name = "execution_count") @Json(name = "execution_count")
  val executionCount: Int?,
  val ename: String?,
  val evalue: String?,
  val traceback: List<String>?,
)

object JupyterCellOutputType {
  const val STREAM = "stream"
  const val DISPLAY_DATA = "display_data"
  const val EXECUTION_RESULT = "execute_result"
  const val ERROR = "error"
}


object JupyterCellType {
  @Suppress("unused")
  const val RAW = "raw"
  const val HEADING = "heading"
  const val CODE = "code"
  const val MARKDOWN = "markdown"
}