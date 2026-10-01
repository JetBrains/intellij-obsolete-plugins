package com.intellij.aiplayground.ui.chat.view

import com.intellij.util.ui.WrapLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import kotlin.math.floor
import kotlin.math.max

/**
 * [WrapLayout] copy that shrinks component below its preferred size if necessary
 */
class ModelsPanelLayoutManager : WrapLayout(LEFT) {

  override fun layoutContainer(target: Container) {
    synchronized(target.treeLock) {
      val insets = target.insets
      val maxwidth = target.width - (insets.left + insets.right + hgap * 2)
      val nmembers = target.componentCount
      var x = 0
      var y = insets.top + vgap
      var rowh = 0
      var start = 0
      val ltr = target.componentOrientation.isLeftToRight
      val useBaseline = alignOnBaseline
      val ascent = IntArray(nmembers)
      val descent = IntArray(nmembers)
      for (i in 0 until nmembers) {
        val m = target.getComponent(i)
        if (m.isVisible) {
          val d = preferredSize(m)
          m.setSize(d.width, d.height)
          if (useBaseline) {
            val baseline = m.getBaseline(d.width, d.height)
            if (baseline >= 0) {
              ascent[i] = baseline
              descent[i] = d.height - baseline
            }
            else {
              ascent[i] = -1
            }
          }
          if (x == 0 || x + d.width <= maxwidth) {
            x += d.width + hgap
            rowh = max(rowh, d.height)
          }
          else {
            rowh = moveComponents(target, insets.left + hgap, y,
                                  maxwidth - x, rowh, start, i, ltr,
                                  useBaseline, ascent, descent)
            x = d.width + hgap
            y += vgap + rowh
            rowh = d.height
            start = i
          }
          if (start == i && x - hgap > maxwidth) {
            m.size = Dimension(maxOf(m.minimumSize.width, maxwidth), d.height)
          }
        }
      }
      moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh,
                     start, nmembers, ltr, useBaseline, ascent, descent)
    }
  }

  private fun preferredSize(m: Component) =
    Dimension(max(m.preferredSize.width, m.minimumSize.width), max(m.preferredSize.height, m.minimumSize.height))

  private fun moveComponents(
    target: Container, _x: Int, y: Int, width: Int, _height: Int,
    rowStart: Int, rowEnd: Int, ltr: Boolean,
    useBaseline: Boolean, ascent: IntArray,
    descent: IntArray,
  ): Int {
    var x = _x
    var height = _height
    when (alignment) {
      LEFT -> x += if (ltr) 0 else width
      CENTER -> x += width / 2
      RIGHT -> x += if (ltr) width else 0
      LEADING -> {
      }
      TRAILING -> x += width
    }
    var maxAscent = 0
    var nonBaselineHeight = 0
    var baselineOffset = 0
    if (useBaseline) {
      var maxDescent = 0
      for (i in rowStart until rowEnd) {
        val m = target.getComponent(i)
        if (m.isVisible) {
          if (ascent[i] >= 0) {
            maxAscent = max(maxAscent, ascent[i])
            maxDescent = max(maxDescent, descent[i])
          }
          else {
            nonBaselineHeight = max(m.height, nonBaselineHeight)
          }
        }
      }
      height = max(maxAscent + maxDescent, nonBaselineHeight)
      baselineOffset = (height - maxAscent - maxDescent) / 2
    }

    var expand = 1.0
    if (fillWidth) {
      var sum = 0.0
      for (i in rowStart until rowEnd) {
        val m = target.getComponent(i)
        if (m.isVisible) {
          sum += max(m.preferredSize.width, m.minimumSize.width)
        }
      }
      expand = target.width.toDouble() / sum
    }

    for (i in rowStart until rowEnd) {
      val m = target.getComponent(i)
      if (m.isVisible) {
        val cy: Int = if (useBaseline && ascent[i] >= 0) {
          y + baselineOffset + maxAscent - ascent[i]
        }
        else {
          y + (height - m.height) / 2
        }
        val w = if (fillWidth) floor(max(m.preferredSize.width, m.minimumSize.width) * expand).toInt() else m.width
        if (ltr) {
          m.setBounds(x, cy, w, m.height)
        }
        else {
          m.setBounds(target.width - x - w, cy, w, m.height)
        }
        x += m.width + hgap
      }
    }
    return height
  }

}