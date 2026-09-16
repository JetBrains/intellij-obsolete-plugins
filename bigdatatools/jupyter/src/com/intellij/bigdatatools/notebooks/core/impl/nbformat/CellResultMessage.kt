package com.intellij.bigdatatools.notebooks.core.impl.nbformat

open class CellResultMessage(val type: CellResultType = CellResultType.NULL,
                             val data: String,
                             val schema: List<SchemaColumn>? = null) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CellResultMessage) return false

    if (type != other.type) return false
    if (data != other.data) return false
    if (schema != other.schema) return false

    return true
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + data.hashCode()
    result = 31 * result + (schema?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "CellResultMessage(type=$type, data='$data', schema=$schema)"
  }


}

data class SchemaColumn(val name: String, val type: String, val metadata: Any)

enum class CellResultType {
  ANGULAR,
  HTML,
  IMG,
  TABLE,
  TEXT,
  NETWORK,
  NULL,
  SVG,

  //Special Bokeh HTML renderer
  BOKEH_HTML
  // MARKDOWN // // ToDo DataBricks special output
}