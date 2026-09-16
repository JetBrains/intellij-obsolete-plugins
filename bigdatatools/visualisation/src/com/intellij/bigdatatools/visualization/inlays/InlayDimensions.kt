package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.ui.scale.JBUIScale
import java.awt.Font

object InlayDimensions {

  /**
   * Offset for inlay painted round-rect background.
   * We need it to draw visual offsets from surrounding text.
   */
  private const val topOffsetUnscaled = 10
  private const val bottomOffsetUnscaled = 24
  private const val rightOffsetUnscaled = 15

  val topOffset = JBUIScale.scale(topOffsetUnscaled)
  val bottomOffset = JBUIScale.scale(bottomOffsetUnscaled)
  val rightOffset = JBUIScale.scale(rightOffsetUnscaled)

  const val topBorderUnscaled = topOffsetUnscaled + 3
  const val bottomBorderUnscaled = bottomOffsetUnscaled + 5
  const val leftBorderUnscaled = 5
  const val rightBorderUnscaled = 17

  /** Real borders for inner inlay component */
  val topBorder = JBUIScale.scale(topBorderUnscaled)
  val bottomBorder = JBUIScale.scale(bottomBorderUnscaled)
  //val leftBorder = JBUIScale.scale(leftBorderUnscaled)
  //val rightBorder = JBUIScale.scale(rightBorderUnscaled)

  val cornerRadius = JBUIScale.scale(10)

  /** editor.lineHeight */
  var lineHeight: Int = JBUIScale.scale(10)
    private set

  /** Width of space character ib current editor (editor.getFontMetrics(Font.PLAIN).charWidth(' ')) */
  private var spaceWidth: Int = JBUIScale.scale(5)

  var smallHeight: Int = lineHeight
    private set

  private var previewHeight: Int = lineHeight
    private set

  var chartHeight: Int = lineHeight
    private set

  var defaultHeight: Int = lineHeight
    private set

  var maxHeight: Int = lineHeight
    private set

  var width: Int = spaceWidth * 120
    private set

  private var minWidth: Int = spaceWidth * 10
    private set

  var minHeight: Int = spaceWidth * 10
    private set

  private var initialized = false

  fun init(editor: EditorImpl) {

    if (initialized) {
      return
    }

    lineHeight = editor.lineHeight + JBUIScale.scale(5)
    spaceWidth = editor.getFontMetrics(Font.PLAIN).charWidth(' ')

    val bordersHeight = topBorder + bottomBorder

    smallHeight = lineHeight + bordersHeight
    previewHeight = lineHeight * 4 + bordersHeight
    chartHeight = lineHeight * 10 + bordersHeight
    defaultHeight = lineHeight * 10 + bordersHeight
    maxHeight = lineHeight * 35 + bordersHeight

    width = spaceWidth * 120
    minWidth = spaceWidth * 10
    minHeight = smallHeight
  }
}