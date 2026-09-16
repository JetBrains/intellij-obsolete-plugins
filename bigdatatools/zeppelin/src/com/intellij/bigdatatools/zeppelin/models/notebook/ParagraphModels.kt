package com.intellij.bigdatatools.zeppelin.models.notebook

import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType

data class ParagraphOutput(val data: String, val index: Int,
                           val noteId: String,
                           val paragraphId: String,
                           val type: CellResultType? = CellResultType.TEXT)

data class ParagraphSettings(
  val params: Map<String, Any?> = hashMapOf(),
  val forms: Map<String, Any?> = hashMapOf()
)
