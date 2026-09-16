package com.intellij.bigdatatools.zeppelin.editor.ui

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ui.components.ActionLink
import org.jetbrains.annotations.Nls
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

class ExecutionProgressPanel : JPanel() {

  private val label = JLabel()
  private val linkLabel = ActionLink("")
  private val progressBar = JProgressBar(0, 100)
  private val ztoolsLabel = JLabel()

  init {
    progressBar.isIndeterminate = true

    progressBar.isVisible = false
    linkLabel.isVisible = false
    ztoolsLabel.isVisible = false
    add(label)
    add(linkLabel)
    add(progressBar)
    add(ztoolsLabel)
  }

  fun setLinkLabelAction(action: () -> Unit) {
    linkLabel.addActionListener { action() }
  }

  fun setLinkLabel(cellIndex: Int) {
    if (cellIndex == -1) {
      linkLabel.isVisible = false
    }
    else {
      linkLabel.isVisible = true
      linkLabel.text = ZepMessagesBundle.message("execution.progress.link", cellIndex + 1)
    }
  }

  fun setLinkLabel(left: Int, right: Int) {
    linkLabel.isVisible = true
    linkLabel.text = "$left/$right "
  }

  fun setIcon(icon: Icon?) {
    label.isVisible = true
    label.icon = icon
  }

  fun setLabel(@Nls text: String) {
    label.isVisible = true
    label.text = text
  }

  fun setZtoolsText(@Nls text: String) {
    ztoolsLabel.isVisible = true
    ztoolsLabel.text = text
  }

  fun hideZtools() {
    ztoolsLabel.isVisible = false
  }

  fun hideProgress() {
    progressBar.isVisible = false
  }

  fun hideAll() {
    hideProgress()
    label.isVisible = false
    linkLabel.isVisible = false
  }

  fun setProgress(progress: Int) {
    progressBar.isVisible = true
    if (progress != 0) {
      progressBar.isIndeterminate = false
      progressBar.value = progress
    }
    else {
      progressBar.isIndeterminate = true
    }
  }
}