package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JBCefOsrHandlerBrowser
import com.intellij.ui.scale.JBUIScale
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandlerAdapter
import java.awt.Component
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import kotlin.math.floor

open class JcefPanel : JComponent() {
  private var imgWidth = 1
  private var imgHeight = 1

  // We cannot use JBHiDPIScaledImage because we need to paint one hidpi image to another and this does not works well with JBHiDPIScaledImage.
  @Suppress("UndesirableClassUsage")
  internal var img: BufferedImage = BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)

  private val handler = JcefPanelRenderHandler(this)
  internal val browser: JBCefBrowserBase
  private val cefBrowser: CefBrowser

  init {
    val client = JBCefApp.getInstance().createClient().apply {
      setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 4)
    }

    // Fix for exception during CEF init on MacOS https://youtrack.jetbrains.com/issue/BDIDE-2123
    //browser = if (SystemInfo.isMac) JBCefOsrHandlerBrowserFixed(client, "", this, true)
    //else JBCefOsrHandlerBrowser.create("", this, client, true)
    browser = JBCefOsrHandlerBrowser.create("", handler, client, true)

    cefBrowser = browser.cefBrowser
    // To prevent native popup menu.
    client.addContextMenuHandler(object : CefContextMenuHandlerAdapter() {
      override fun onBeforeContextMenu(browser: CefBrowser?, frame: CefFrame?, params: CefContextMenuParams?, model: CefMenuModel?) {
        model?.clear()
      }

      override fun onContextMenuCommand(
        browser: CefBrowser?,
        frame: CefFrame?,
        params: CefContextMenuParams?,
        commandId: Int,
        eventFlags: Int,
      ) = false

      override fun onContextMenuDismissed(browser: CefBrowser?, frame: CefFrame?) {}
    }, cefBrowser)

    addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent) = forwardKeyEvent(e)
      override fun keyPressed(e: KeyEvent) = forwardKeyEvent(e)
      override fun keyReleased(e: KeyEvent) = forwardKeyEvent(e)
    })

    addMouseMotionListener(object : MouseMotionListener {
      override fun mouseDragged(e: MouseEvent) = forwardMouseEvent(e)
      override fun mouseMoved(e: MouseEvent) = forwardMouseEvent(e)
    })

    addMouseWheelListener {
      forwardMouseWheelEvent(it)
    }

    addMouseListener(object : MouseListener {
      override fun mouseClicked(e: MouseEvent) = forwardMouseEvent(e)
      override fun mousePressed(e: MouseEvent) = forwardMouseEvent(e)
      override fun mouseReleased(e: MouseEvent) = forwardMouseEvent(e)
      override fun mouseEntered(e: MouseEvent) = forwardMouseEvent(e)
      override fun mouseExited(e: MouseEvent) = forwardMouseEvent(e)
    })
  }

  private fun forwardKeyEvent(e: KeyEvent) {
    requestFocus()
    cefBrowser.sendKeyEvent(e)
  }

  private fun forwardMouseWheelEvent(e: MouseWheelEvent) {
    if (!SystemInfo.isLinux) {
      cefBrowser.sendMouseWheelEvent(e)
    }
    else {
      val newEvent = MouseWheelEvent(
        e.source as Component,
        e.id,
        e.`when`,
        e.modifiersEx,
        e.x,
        e.y,
        e.clickCount,
        e.isPopupTrigger,
        e.scrollType,
        e.scrollAmount,
        -(e.wheelRotation * JBUIScale.sysScale().toInt()))

      cefBrowser.sendMouseWheelEvent(newEvent)
    }
  }

  private fun forwardMouseEvent(e: MouseEvent) {
    requestFocus()
    cefBrowser.sendMouseEvent(e)
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    super.setBounds(x, y, width, height)

    if (imgWidth == width && imgHeight == height || width <= 0 || height <= 0) {
      return
    }

    val oldImage = img

    imgWidth = width
    imgHeight = height

    // We cannot use JBHiDPIScaledImage because we need to paint one hidpi image to another,
    // and this does not work well with JBHiDPIScaledImage.
    @Suppress("UndesirableClassUsage")
    img = BufferedImage(floor(JBUIScale.sysScale() * width).toInt(), floor(JBUIScale.sysScale() * height).toInt(),
                        BufferedImage.TYPE_INT_ARGB)

    val g = img.graphics

    try {
      g.drawImage(oldImage, 0, 0, null)
    }
    catch (_: Exception) {
      //
    }
    finally {
      g.dispose()
    }

    cefBrowser.wasResized(img.width, img.height)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.drawImage(img, 0, 0, width, height, null)
  }
}