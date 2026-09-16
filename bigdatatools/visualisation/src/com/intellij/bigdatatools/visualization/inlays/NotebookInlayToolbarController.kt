package com.intellij.bigdatatools.visualization.inlays

import java.awt.Component
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JProgressBar

/**
 * Controls inlay top part, with status label, progressbar.
 */
class NotebookInlayToolbarController(private val inlay: NotebookInlayComponent) {

  fun showProgress() {
    getOrAddProgress().apply {
      if (!isIndeterminate) {
        isIndeterminate = true
      }
    }
  }

  fun onProgress(percentage: Int) {
    getOrAddProgress().apply {
      if (isIndeterminate) {
        isIndeterminate = false
      }
      value = percentage
    }
  }

  fun hideProgress() {
    inlay.top.components.firstOrNull { it is JProgressBar }?.let {
      inlay.top.remove(it)
      inlay.outputController.adjustHeight()
    }
  }

  fun showCollapsedState(value: String?) {
    getOrAddLabel(value)
    hideProgress()
  }

  fun removeCollapsedState() {
    inlay.top.components.firstOrNull { it is JLabel }?.let {
      inlay.top.remove(it)
      inlay.outputController.adjustHeight()
    }
  }

  fun setDescription(value: String) {
    getOrAddLabel(value)
  }

  fun removeDescription() {
    inlay.top.components.firstOrNull { it is JLabel }?.let {
      inlay.top.remove(it)
      inlay.outputController.adjustHeight()
    }
  }

  fun hide() {
    if (inlay.top.componentCount != 0) {
      inlay.top.removeAll()
      inlay.outputController.adjustHeight()
    }
  }

  private fun getOrAddProgress(): JProgressBar {
    var progressBar = inlay.top.components.firstOrNull { it is JProgressBar } as JProgressBar?
    if (progressBar != null) {
      return progressBar
    }

    progressBar = JProgressBar(0, 100).apply {
      alignmentX = Component.LEFT_ALIGNMENT
    }
    inlay.top.add(progressBar, 0)
    inlay.outputController.adjustHeight()
    return progressBar
  }

  private fun getOrAddLabel(value: String?): JLabel {
    var label = inlay.top.components.firstOrNull { it is JLabel } as JLabel?
    if (label != null) {
      @Suppress("HardCodedStringLiteral")
      label.text = value
      return label
    }

    @Suppress("HardCodedStringLiteral")
    label = JLabel(value).apply {
      isEnabled = false
      alignmentX = Component.LEFT_ALIGNMENT
      font = inlay.editor.getFontMetrics(Font.PLAIN).font
    }

    inlay.top.add(label)
    inlay.top.revalidate()
    inlay.outputController.adjustHeight()
    return label
  }
}