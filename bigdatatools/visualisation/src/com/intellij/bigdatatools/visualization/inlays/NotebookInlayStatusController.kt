package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.impl.serialize.NotebookImportDeserializer
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.charts.utils.getAsStringOrNull
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.util.TimeUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JLabel

/**
 * Controls inlay bottom part, with status label.
 */
class NotebookInlayStatusController(private val inlay: NotebookInlayComponent) {

  /**
   * Text that will be drawn in the bottom left corner.
   * Usually contains last modified date, author and execution duration.
   */
  private var statusLabel: JLabel? = null

  private fun setStatusLabel(text: String) {
    var statusLabel = statusLabel
    if (statusLabel != null) {
      @Suppress("HardCodedStringLiteral")
      statusLabel.text = text
      return
    }

    val editor = inlay.editor

    @Suppress("HardCodedStringLiteral")
    statusLabel = JLabel(text).apply {
      font = editor.component.font.deriveFont(editor.component.font.size2D * 0.75f)
      foreground = JBUI.CurrentTheme.Label.disabledForeground(false)
    }

    if (!inlay.collapsed) {
      inlay.bottom.add(statusLabel, 0)
      inlay.outputController.adjustHeight()
    }

    this.statusLabel = statusLabel
  }

  private fun removeStatusLabel() {
    statusLabel?.let {
      inlay.bottom.remove(it)
      statusLabel = null
      inlay.outputController.adjustHeight()
    }
  }

  fun detachStatusLabel() = statusLabel?.let { inlay.bottom.remove(it) }
  fun attachStatusLabel() = statusLabel?.let { inlay.bottom.add(it, 0) }

  // This is how the result text will look like
  // Took 18 sec. Last updated by anonymous at October 22 2019, 4:07:25 PM.
  // Last updated by anonymous at October 22 2019, 4:07:08 PM.
  // Last updated at October 22 2019, 4:07:08 PM.

  // Fields from json
  // "user": "admin",
  // "dateUpdated": "2019-08-19T18:01:59+0300"
  // "dateCreated": "2019-09-17T19:12:21+0300"
  // "dateStarted": "2019-09-24T15:32:15+0300"
  // "dateFinished": "2019-09-24T15:32:15+0300"
  fun updateStatus() {

    if (!InlaysConfig.getInstance().showBottomInfo) {
      removeStatusLabel()
      return
    }

    val json = inlay.cell.asJsonTree()

    val dateUpdated = getDate(json.getAsStringOrNull("dateUpdated"))
    //val dateCreated = getDate(json["dateCreated"].asString)
    val dateStarted = getDate(json.getAsStringOrNull("dateStarted"))
    val dateFinished = getDate(json.getAsStringOrNull("dateFinished"))

    sb.setLength(0)
    if (dateFinished != null && dateStarted != null) {
      val delta = dateFinished.time - dateStarted.time

      if (delta > 0) {
        sb.append("Took ").append(TimeUtils.intervalAsString(delta)).append(".")
      }
    }

    if (json.has("user") || dateUpdated != null) {
      sb.append(" Last updated")
      if (json.has("user")) {
        sb.append(" by ${json["user"].asString}")
      }

      if (dateUpdated != null) {
        sb.append(" at ${dateUpdated}")
      }
    }

    if (sb.isNotBlank()) {
      setStatusLabel(sb.toString())
    }
    else {
      removeStatusLabel()
    }
  }

  companion object {

    private val DATE_FORMATS = NotebookImportDeserializer.DATE_FORMATS.map { SimpleDateFormat(it, Locale.US) }

    private val sb = StringBuilder()

    // For printing cell info about started / finished / updated dates.
    private fun getDate(value: String?): Date? {

      if (value.isNullOrEmpty()) {
        return null
      }

      for (format in DATE_FORMATS) {
        try {
          return format.parse(value)
        }
        catch (e: ParseException) {
        }
      }
      return null
    }
  }
}