package com.intellij.bigdatatools.notebooks.core.api.nbformat

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import org.jetbrains.annotations.Nls

enum class CellStatus(@Nls val text: String) {
  UNKNOWN(NoteMessagesBundle.message("cell.status.unknown")),
  READY(NoteMessagesBundle.message("cell.status.ready")),
  PENDING(NoteMessagesBundle.message("cell.status.pending")),
  RUNNING(NoteMessagesBundle.message("cell.status.running")),
  FINISHED(NoteMessagesBundle.message("cell.status.finished")),
  ERROR(NoteMessagesBundle.message("cell.status.error")),
  ABORT(NoteMessagesBundle.message("cell.status.abort"));

  val isFinished: Boolean
    get() = this in setOf(FINISHED, ERROR, ABORT)
}