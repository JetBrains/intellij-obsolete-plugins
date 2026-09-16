package com.intellij.bigdatatools.notebooks.style

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle

enum class LinesNumberingMode(val title: String) {
  DOCUMENT(NoteMessagesBundle.message("style.linesNumbering.document")),
  CELL(NoteMessagesBundle.message("style.linesNumbering.cell"))
}