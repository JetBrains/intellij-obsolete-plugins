package com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model

import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType

enum class JupyterOutputType(val type: String) {
  TEXT_PLAIN("text/plain"),
  IMAGE_PNG("image/png"),
  LATEX("text/latex"),
  SVG_XML("image/svg+xml"),
  TEXT_HTML("text/html"),
  APPLICATION_JAVASCRIPT("application/javascript");

  val zeppelinType: CellResultType
    get() = (if (TEXT_PLAIN == this)
      CellResultType.TEXT
    else
      CellResultType.HTML)

  override fun toString() = type
}