package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterStatus
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.JLabel

class StatusLabel : JLabel() {

  companion object {
    private val TEXT_COLOR_READY = JBColor(Color(54, 135, 70), Color(80, 166, 97))
    private val TEXT_COLOR_ERROR = JBColor(Color(199, 34, 45), Color(255, 82, 97))
    private val TEXT_COLOR_PROGRESS = JBColor(Color(128, 128, 128), Color(140, 140, 140))

    private val DOT_COLOR_READY = Color(98, 181, 67)
    private val DOT_COLOR_ERROR = Color(224, 85, 85)
    private val DOT_COLOR_NOT_CREATED = Color(154, 167, 176, 128)
    private val DOT_COLOR_PROGRESS = Color(244, 175, 61)
  }

  private val statusIcon = ColoredCircleIcon()

  var status: InterpreterStatus = InterpreterStatus.NOT_CREATED
    set(value) {
      if (field == value)
        return

      field = value

      statusIcon.color = when (value) {
        InterpreterStatus.NOT_CREATED -> DOT_COLOR_NOT_CREATED
        InterpreterStatus.DOWNLOADING_DEPENDENCIES -> DOT_COLOR_PROGRESS
        InterpreterStatus.ERROR -> DOT_COLOR_ERROR
        InterpreterStatus.PENDING -> DOT_COLOR_PROGRESS
        InterpreterStatus.READY -> DOT_COLOR_READY
      }

      foreground = when (value) {
        InterpreterStatus.NOT_CREATED -> TEXT_COLOR_PROGRESS
        InterpreterStatus.DOWNLOADING_DEPENDENCIES -> TEXT_COLOR_PROGRESS
        InterpreterStatus.ERROR -> TEXT_COLOR_ERROR
        InterpreterStatus.PENDING -> TEXT_COLOR_PROGRESS
        InterpreterStatus.READY -> TEXT_COLOR_READY
      }

      text = when (value) {
        InterpreterStatus.READY -> ZepMessagesBundle.message("interpreter.status.ready")
        InterpreterStatus.NOT_CREATED -> ZepMessagesBundle.message("interpreter.status.notCreating")
        InterpreterStatus.DOWNLOADING_DEPENDENCIES -> ZepMessagesBundle.message("interpreter.status.downloadingDependencies")
        InterpreterStatus.ERROR -> ZepMessagesBundle.message("interpreter.status.error")
        InterpreterStatus.PENDING -> ZepMessagesBundle.message("interpreter.status.pending")
      }
    }

  init {
    icon = statusIcon
  }
}