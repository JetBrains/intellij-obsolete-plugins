package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.visualization.inlays.InlayDimensions
import com.intellij.bigdatatools.visualization.inlays.components.GithubMarkdownCss
import com.intellij.bigdatatools.visualization.inlays.components.JcefPanel
import com.intellij.bigdatatools.visualization.inlays.settings.InlaysSettings
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ComponentUtil
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JBCefPsiNavigationUtils.navigateTo
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.MouseEventAdapter
import com.jetbrains.bigdatatools.common.util.invokeLater
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.JScrollPane
import kotlin.io.path.name
import kotlin.math.min

open class InlayHtmlOffscreenPage : InlayPage {

  override var indexInResults: Int = -1

  private var data = ""

  final override val component: JcefPanel = object : JcefPanel() {
    override fun updateUI() {
      if (loaded) {
        addData(data)
      }
    }
  }

  private val browser = component.browser

  override val title: String
    get() = VisMessagesBundle.message("page.title.html")

  override val contentType: InlayPageContentType = InlayPageContentType.HTML

  private var cannotScroll = false

  private var loaded = false
  private var scrollDetectionTimer: Future<*>? = null
  private var runScrollDetectionAfterLoad = false

  private var queryGetsScrollHeightDiff: JBCefJSQuery? = null
  private var queryHeight: JBCefJSQuery? = null
  private var queryOpenInBrowser: JBCefJSQuery? = null
  private var queryOnLoad: JBCefJSQuery? = null
  private var scriptsInitialized = false

  init {
    Disposer.register(this, browser)

    component.addMouseWheelListener { e ->
      if (cannotScroll) {
        MouseEventAdapter.redispatch(e, ComponentUtil.getParentOfType(JScrollPane::class.java as Class<out JScrollPane?>, component.parent))
      }
    }

    component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        if (loaded) {
          scheduleScrollDetection()
        }
        else {
          runScrollDetectionAfterLoad = true
        }
      }
    })

    browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
      override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {

        // Method onLoadEnd called twice, first with httpStatusCode == 0 and second with httpStatusCode == 200. When its called first time,
        // nothing is really loaded.
        if (httpStatusCode != 200) {
          return
        }

        loaded = true

        initJS()

        queryOpenInBrowser?.let {
          // Open links in external browser.
          browser.executeJavaScript("""
          window.JavaPanelBridge = {
            openInExternalBrowser : function(link) {
                ${it.inject("link")} 
            }
          };""", browser.url, 0)
        }

        queryOnLoad?.let {
          browser.executeJavaScript(
            """
             if(window.loaded == true) {
               ${it.inject("'loaded'")}
             } else {
               window.onload = function(e){
                 ${it.inject("'onload'")}
               }
             }
           """, browser.url, 0)
        }

        if (runScrollDetectionAfterLoad) {
          runScrollDetectionAfterLoad = false
          scheduleScrollDetection()
        }
      }
    }, browser.cefBrowser)
  }

  private fun initJS() {

    if (scriptsInitialized) {
      return
    }

    scriptsInitialized = true

    val queryGetsScrollHeightDiff = JBCefJSQuery.create(browser)
    val queryHeight = JBCefJSQuery.create(browser)
    val queryOpenInBrowser = JBCefJSQuery.create(browser)
    val queryOnLoad = JBCefJSQuery.create(browser)

    queryGetsScrollHeightDiff.addHandler {
      val scrollHeightDiff = it.toIntOrNull() ?: 0
      cannotScroll = scrollHeightDiff <= 0
      scrollDetectionTimer = null
      null
    }
    Disposer.register(browser, queryGetsScrollHeightDiff)
    this.queryGetsScrollHeightDiff = queryGetsScrollHeightDiff

    queryHeight.addHandler {
      val preferredHeight = it.toIntOrNull() ?: 0
      invokeLater {
        component.preferredSize = Dimension(component.preferredSize.width, min(InlayDimensions.maxHeight, preferredHeight))
      }
      null
    }
    Disposer.register(browser, queryHeight)
    this.queryHeight = queryHeight

    queryOnLoad.addHandler {
      invokeLater {
        requestHeight()
      }
      null
    }
    Disposer.register(browser, queryOnLoad)
    this.queryOnLoad = queryOnLoad

    queryOpenInBrowser.addHandler { link ->
      if (navigateTo(link)) return@addHandler null
      BrowserUtil.browse(link)
      null
    }
    Disposer.register(browser, queryOpenInBrowser)
    this.queryOpenInBrowser = queryOpenInBrowser
  }

  private fun requestHeight() {
    if (queryHeight?.isDisposed == true) {
      return
    }

    val queryHeight = queryHeight ?: return
    browser.cefBrowser.executeJavaScript("""
                                             var body = document.body;
                                             var html = document.documentElement;
                                             var bodyScrollHeight = 0;
                                             var bodyOffsetHeight = 0;
                                             var htmlClientHeight = 0;
                                             var htmlScrollHeight = 0;
                                             var htmlOffsetHeight = 0;
                                             if(body != null) {
                                               bodyScrollHeight = body.scrollHeight;
                                               bodyOffsetHeight = body.offsetHeight;
                                             }
                                             if(html != null) {
                                               htmlClientHeight = html.clientHeight;
                                               htmlScrollHeight = html.scrollHeight;
                                               htmlOffsetHeight = html.offsetHeight;
                                             }
                                             var height =  Math.max(bodyScrollHeight, bodyOffsetHeight, htmlClientHeight, htmlScrollHeight, htmlOffsetHeight);
                                             ${queryHeight.inject("height")}
                                             """.trimIndent(),
                                         browser.cefBrowser.url, 0)
  }

  override fun getCollapsedDescription(): String = "Html page"

  override fun createActions(): List<AnAction> {
    val actionSaveAsTxt = DumbAwareAction.create(VisMessagesBundle.message("html.saveAs.text"), AllIcons.Actions.MenuSaveall) {
      saveAsHtml()
    }
    return listOf(actionSaveAsTxt)
  }

  override fun dispose() {
    super.dispose()
    cancelScrollDetection()
  }

  private fun saveAsHtml() {
    val descriptor = FileSaverDescriptor(VisMessagesBundle.message("html.exportAs.text"),
                                         VisMessagesBundle.message("html.exportAs.hint"), "html")
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, component)
    val exportPath = InlaysSettings.getInstance().getHtmlsExportPath()
    val fileWrapper = chooser.save(exportPath.parent, exportPath.name) ?: return
    InlaysSettings.getInstance().htmlExportPath = fileWrapper.file.path

    ApplicationManager.getApplication().runWriteAction {
      browser.cefBrowser.getSource { data ->
        fileWrapper.file.bufferedWriter().use { out ->
          out.write(data)
        }
      }
    }
  }

  private fun cancelScrollDetection() {
    scrollDetectionTimer?.cancel(true)
    scrollDetectionTimer = null
  }

  fun clear() {
    data = ""
    browser.loadHTML("")
    cancelScrollDetection()
    loaded = false
  }

  private fun scheduleScrollDetection() {
    if (scrollDetectionTimer != null) {
      return
    }

    val queryGetsScrollHeightDiff = queryGetsScrollHeightDiff ?: return

    scrollDetectionTimer = AppExecutorUtil.getAppScheduledExecutorService().schedule({
                                                                                       try {
                                                                                         browser.cefBrowser.executeJavaScript(
                                                                                           "var body = document.body;" +
                                                                                           "var heightDiff = 0;" +
                                                                                           "if(body != null) { heightDiff = document.body.scrollHeight - document.body.clientHeight; }" +
                                                                                           queryGetsScrollHeightDiff.inject("heightDiff"),
                                                                                           browser.cefBrowser.url, 0)
                                                                                       }
                                                                                       catch (_: Exception) {
                                                                                         cancelScrollDetection()
                                                                                       }
                                                                                     }, 1, TimeUnit.SECONDS)
  }

  open fun addData(data: String) {
    loaded = false
    this.data = data
    cancelScrollDetection()

    browser.loadHTML(StringBuilder().append("<!DOCTYPE html><html><head><style>")
                       .append(GithubMarkdownCss.getCss(EditorColorsManager.getInstance().globalScheme.defaultBackground))
                       .append("</style>")
                       .append("""
      <script language="javascript">
      window.onload = function(e){
           window.loaded=true;
      }
      </script>
    """)
                       .append("</head><body>")
                       .append(data)
                       .append("</body></html>").toString())
  }
}