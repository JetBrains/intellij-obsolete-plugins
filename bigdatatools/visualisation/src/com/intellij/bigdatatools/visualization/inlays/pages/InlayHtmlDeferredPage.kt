package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.coreUi.util.messageOrDefault
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.concurrency.AsyncPromise
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTextArea

/** Used for Bokeh rendering. */
class InlayHtmlDeferredPage(private val data: String, promise: AsyncPromise<String>, private val realPage: InlayPage) : InlayPage {

  override var indexInResults = -1

  override val title: String
    get() = realPage.title

  override val contentType = InlayPageContentType.IMAGE

  override val component = JPanel(BorderLayout())

  init {
    val loadingDisposable = Disposer.newDisposable()
    Disposer.register(this, loadingDisposable)
    Disposer.register(this, realPage)

    val panel = JBLoadingPanel(BorderLayout(), loadingDisposable)
    panel.setLoadingText(VisMessagesBundle.message("html.loading"))
    component.add(panel, BorderLayout.CENTER)
    panel.startLoading()
    component.preferredSize = Dimension(component.preferredSize.width, JBUIScale.scale(150))

    promise.onSuccess {
      panel.stopLoading()
      Disposer.dispose(loadingDisposable)
      component.removeAll()
      component.add(realPage.component, BorderLayout.CENTER)
      if (realPage is InlayHtmlOffscreenPage) realPage.addData(it + data)

      NotebookInlayUtils.doWhenPreferredSizeSet(realPage.component, once = false) {
        component.preferredSize = realPage.component.preferredSize
      }
    }.onError {
      panel.stopLoading()
      val textArea = JTextArea("${VisMessagesBundle.message("bokeh.loading.async.error")}\n${it.messageOrDefault()}")
      textArea.isEditable = false
      panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
    }
  }

  override fun getCollapsedDescription(): String? {
    return realPage.getCollapsedDescription()
  }

  override fun createActions(): List<AnAction> = emptyList()
}