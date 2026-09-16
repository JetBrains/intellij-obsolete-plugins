package com.intellij.bigdatatools.notebooks.core.impl.editor

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants

object NotebookCellUtil {
  private val regex = Regex("^%(((\\S+\\.)?(\\S+))|([ \\t]?))")

  fun toText(source: String) = source.removePrefix(NotebookConstants.PARAGRAPH_DELIMITER)

  fun toSource(rawText: String): String {
    val text = rawText.replace("\r\n", "\n")
    return NotebookConstants.PARAGRAPH_DELIMITER + text
  }

  fun getMarkerText(s: String): String {
    return regex.find(s)?.value ?: return ""
  }

  fun calcInterpreterCode(marker: String) = marker
    .removePrefix(NotebookConstants.INTERPRETER_MARKER)
    .trim()
}