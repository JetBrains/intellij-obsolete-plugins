package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterInfo
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.Nls
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JLabel

/** Special label, drawn on rounded rect background, with interpreter name as text and some html-formatted  interpreter info in hint. */
class InterpreterInfoLabel(@Nls text: String, interpreterInfo: InterpreterInfo) : JLabel(text) {

  companion object {
    private val RADIUS = JBUIScale.scale(6)
  }

  init {
    border = JBUI.Borders.empty(3)

    val sb = StringBuilder().append("<html>")
      .append(ZepMessagesBundle.message("interpreter.info.className")).append(" <b>").append(interpreterInfo.className).append("</b>")
      .append("<br>").append(ZepMessagesBundle.message("interpreter.info.default")).append(" <b>").append(
        interpreterInfo.defaultInterpreter).append("</b>")

    if (interpreterInfo.editor.isNotEmpty()) {
      sb.append("<br><br>").append(ZepMessagesBundle.message("interpreter.info.editor"))
      interpreterInfo.editor.forEach {
        sb.append("<br>").append("• ").append(it.key).append(": <b>").append(it.value).append("</b>")
      }
    }

    if (interpreterInfo.config.isNotEmpty()) {
      sb.append("<br><br>").append(ZepMessagesBundle.message("interpreter.info.config"))
      interpreterInfo.config.forEach {
        sb.append("<br>").append("• ").append(it.key).append(": <b>").append(it.value).append("</b>")
      }
    }

    sb.append("</html>")

    @Suppress("HardCodedStringLiteral") // Here we have a combination of remote interpreter names groups and so on.
    toolTipText = sb.toString()
  }

  override fun paintComponent(g: Graphics) {

    val g2d = g.create() as Graphics2D

    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.color = JBUI.CurrentTheme.TabbedPane.FOCUS_COLOR
      g2d.fillRoundRect(0, 0, width, height, RADIUS, RADIUS)
    }
    finally {
      g2d.dispose()
    }

    super.paintComponent(g)
  }
}