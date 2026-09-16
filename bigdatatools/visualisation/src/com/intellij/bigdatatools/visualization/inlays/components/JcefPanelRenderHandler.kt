package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.ui.scale.JBUIScale
import com.jetbrains.cef.SharedMemoryCache
import org.cef.browser.CefBrowser
import org.cef.callback.CefDragData
import org.cef.handler.CefNativeRenderHandler
import org.cef.handler.CefScreenInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class JcefPanelRenderHandler(private val panel: JcefPanel) : CefNativeRenderHandler {

  private val sharedMemCache: SharedMemoryCache = SharedMemoryCache()

  override fun disposeNativeResources(): Unit = Unit

  override fun getViewRect(p0: CefBrowser?): Rectangle {
    // ToDo max(1, width) and  max(1, height) - hack to prevent crash on zero size.
    return Rectangle(0, 0, max(1, panel.width), max(1, panel.height))
  }

  override fun getScreenInfo(browser: CefBrowser?, screenInfo: CefScreenInfo): Boolean {
    val viewRect = getViewRect(browser)
    screenInfo.Set(JBUIScale.sysScale().toDouble(), 32, 4, false, viewRect, viewRect)
    return true
  }

  override fun getScreenPoint(browser: CefBrowser?, viewPoint: Point): Point = viewPoint

  override fun getDeviceScaleFactor(p0: CefBrowser?): Double = JBUIScale.sysScale().toDouble()

  override fun onPaintWithSharedMem(browser: CefBrowser?, popup: Boolean, dirtyRectsCount: Int, sharedMemName: String?, boostHandle: Long, width: Int, height: Int) {
    if (sharedMemName == null)
      return

    val mem = sharedMemCache.get(sharedMemName, boostHandle).apply {
      this.width = width
      this.height = height
      this.dirtyRectsCount = dirtyRectsCount
      lasUsedMs = System.currentTimeMillis()
    }

    val buffer = mem.wrapRaster()

    val dirtyRects = arrayOf(Rectangle(0, 0, width, height))
    if (dirtyRectsCount > 0) {
      val rectsMem = mem.wrapRects()
      val rects = rectsMem.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
      for (c in 0..dirtyRectsCount - 1) {
        var pos = c * 4
        val r = Rectangle()
        r.x = rects.get(pos++)
        r.y = rects.get(pos++)
        r.width = rects.get(pos++)
        r.height = rects.get(pos)
        dirtyRects[c] = r
      }
    }

    onPaint(browser, popup, dirtyRects, buffer, width, height)
  }

  override fun onPaint(browser: CefBrowser?, popup: Boolean, dirtyRects: Array<Rectangle>, buffer: ByteBuffer, width: Int, height: Int) {

    val img = panel.img
    if (popup || width != img.width || height != img.height) {
      return
    }

    // Old approach
    // val rect = dirtyRects.first()
    // val source = toArray(buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer())
    // img.setRGB(rect.x, rect.y, rect.width, rect.height, source, rect.y * width + rect.x, width)

    val source = buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    val dest = img.raster.dataBuffer as DataBufferInt

    for (rect in dirtyRects) {
      val rectBottom = rect.y + rect.height
      val rectRight = rect.x + rect.width

      // Copy only data from dirty rects.
      for (y in rect.y until rectBottom) {
        val lineStart = (y * img.width)
        val lineEnd = lineStart + rectRight

        val from = lineStart + rect.x
        val length = lineEnd - from
        source.position(from).get(dest.data, from, length)
      }
    }

    panel.repaint()
  }

  override fun onPopupShow(browser: CefBrowser?, show: Boolean): Unit = Unit
  override fun onPopupSize(browser: CefBrowser?, size: Rectangle?): Unit = Unit
  override fun onCursorChange(browser: CefBrowser?, cursor: Int): Boolean = false
  override fun startDragging(browser: CefBrowser?, dragData: CefDragData?, mask: Int, x: Int, y: Int): Boolean = false
  override fun updateDragCursor(browser: CefBrowser?, operation: Int): Unit = Unit
}