// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.components

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphTitleController
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.editor.Editor

open class ParagraphItem(val cell: ZeppelinCell, val editor: Editor) {

  val title = "${ZepMessagesBundle.message("paragraph.name")} ${
    ZeppelinParagraphTitleController.generateTitle(cell)
  } ${if (cell.interpreterCode.isBlank()) "" else "(${cell.interpreterCode})"}"

  val description: String = calculateDescription()

  private fun calculateDescription(): String {
    val range = NotebookEditorUtils.getCellRangeInEditor(editor, cell)

    val shift = cell.indexInNote
    val startLine = editor.offsetToLogicalPosition(range.startOffset).line - shift
    val endLine = editor.offsetToLogicalPosition(range.endOffset).line - shift
    return "Lines: $startLine - $endLine"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ParagraphItem

    return cell == other.cell
  }

  override fun hashCode(): Int = cell.hashCode()
  override fun toString(): String = title
}