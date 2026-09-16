/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.bigdatatools.databricks.toolwindow

import com.intellij.ui.components.JBPanel
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Rectangle
import java.awt.RenderingHints
import kotlin.math.roundToInt

// Copy of com.android.tools.adtui.ui.ImagePanel with small rework.

/**
 * A [JBPanel] that shows an [Image] as background, scaled to fit preserving the aspect ratio.
 */
class ImagePanel : JBPanel<ImagePanel>(true) {
  var image: Image? = null
    set(value) {
      field = value
      repaint()
    }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    paintPanelImage(g, image)
  }
}

/**
 * Draws an image inside a [JBPanel], preserving aspect ratio.
 */
fun <T : JBPanel<T>> JBPanel<T>.paintPanelImage(g: Graphics, image: Image?) {
  val insets = this.insets
  val rect = Rectangle(insets.left, insets.top, width - insets.left - insets.right, height - insets.bottom - insets.top)

  // Paint background.
  g.color = background
  g.fillRect(rect.x, rect.y, rect.width, rect.height)

  // Draw image, centered, preserving the aspect ratio.
  image?.let { img ->
    val imageWidth = img.getWidth(this)
    val imageHeight = img.getHeight(this)
    if (imageWidth > 0 && imageHeight > 0) {
      val scale = (rect.getWidth() / imageWidth).coerceAtMost(rect.getHeight() / imageHeight)
      val w = (imageWidth * scale).roundToInt()
      val h = (imageHeight * scale).roundToInt()
      val xOffset = rect.x + (rect.width - w) / 2
      val yOffset = rect.y + (rect.height - h) / 2

      val g2 = g.create() as Graphics2D
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.drawImage(img, xOffset, yOffset, w, h, null)
      }
      finally {
        g2.dispose()
      }
    }
  }
}