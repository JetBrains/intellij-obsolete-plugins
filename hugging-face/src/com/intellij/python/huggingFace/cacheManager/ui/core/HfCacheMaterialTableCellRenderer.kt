package com.intellij.python.huggingFace.cacheManager.ui.core

import com.intellij.ui.scale.JBUIScale
import java.awt.Component
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.TableCellRenderer

open class HfCacheMaterialTableCellRenderer : JComponent(), TableCellRenderer {
  protected var text = ""
  protected var horizontalAlignment = SwingConstants.LEADING
  private var lastColumn = false

  init {
    isOpaque = true
  }

  override fun getTableCellRendererComponent(table: JTable,
                                             value: Any?,
                                             isSelected: Boolean,
                                             hasFocus: Boolean,
                                             row: Int,
                                             column: Int): Component {
    font = table.font
    setValue(value)
    return this
  }

  open fun setValue(value: Any?) {
    text = value?.toString() ?: ""
  }

  override fun getPreferredSize(): Dimension {
    val fontMetrics = getFontMetrics(font)
    return Dimension(
      geStringWidthLimited(fontMetrics, text) + textLeftOffset * 2,
      fontMetrics.ascent - fontMetrics.descent + textVerticalOffset * 2
    )
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)

    val g2 = g.create() as Graphics2D
    try {
      val aaHint = label.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING)
      if (aaHint != null) {
        configureHint(RenderingHints.KEY_TEXT_ANTIALIASING, g2, aaHint)
        configureHint(RenderingHints.KEY_FRACTIONALMETRICS, g2, label.getClientProperty(RenderingHints.KEY_FRACTIONALMETRICS))
        configureHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, g2, label.getClientProperty(RenderingHints.KEY_TEXT_LCD_CONTRAST))
      }

      g2.color = background
      g2.fillRect(0, 0, width, height)

      g2.font = font

      g2.color = foreground
      val fontMetrics = g2.fontMetrics
      if (horizontalAlignment == SwingConstants.RIGHT && !lastColumn) {
        g2.drawString(getTrimmedToWidthText(fontMetrics, text, width - textLeftOffset),
                      width - textLeftOffset - geStringWidthLimited(fontMetrics, text),
                      (height + fontMetrics.ascent - fontMetrics.descent) / 2)
      }
      else {
        g2.drawString(getTrimmedToWidthText(fontMetrics, text, width - textLeftOffset),
                      textLeftOffset, (height + fontMetrics.ascent - fontMetrics.descent) / 2)
      }
    }
    finally {
      g2.dispose()
    }
  }

  companion object {
    // This label used only to get the proper system hints for TEXT_ANTIALIASING in rendering of our cell. We should find another way.
    private val label = JLabel()

    val textLeftOffset = JBUIScale.scale(5)
    val textVerticalOffset = JBUIScale.scale(8)

    // Return string width in pixels or 1024 if string is wider.
    fun geStringWidthLimited(fontMetrics: FontMetrics, str: String): Int {
      val len = str.length
      return if (len < 128) {
        val data = CharArray(len)
        str.toCharArray(data, 0, 0, len)
        fontMetrics.charsWidth(data, 0, len)
      }
      else {
        var strWidth = 0
        var pos = 0
        do {
          strWidth += fontMetrics.charWidth(str[pos])
          pos++
        }
        while (strWidth < 1024 && pos < len)
        strWidth
      }
    }

    private fun getTrimmedToWidthText(fontMetrics: FontMetrics, text: String, width: Int): String {
      val len = text.length
      if (len < 128) return text

      var strWidth = 0
      var pos = 0
      do {
        strWidth += fontMetrics.charWidth(text[pos])
        pos++
      }
      while (strWidth < width && pos < len)

      return when {
        pos == len -> text
        pos - 3 <= 0 -> "..."
        else -> {
          val substring = text.substring(0, pos - 3)
          "$substring..."
        }
      }
    }

    private fun configureHint(key: RenderingHints.Key, g: Graphics2D, newValue: Any?) {
      if (newValue == null) return  // new value is not provided
      if (newValue == g.getRenderingHint(key)) return  // value is not changed
      g.setRenderingHint(key, newValue)
    }
  }
}