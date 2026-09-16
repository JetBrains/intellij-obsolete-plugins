package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataProvider
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.visualization.inlays.InlayDimensions
import com.intellij.bigdatatools.visualization.inlays.components.ColoredTextConsole
import com.intellij.bigdatatools.visualization.inlays.settings.InlaysSettings
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.io.path.name
import kotlin.math.max
import kotlin.math.min

internal class InlayTextPage(private val project: Project, editor: EditorImpl, cell: NotebookCell) : InlayPage {
  companion object {
    const val SELECT_ALL_ACTION_KEY = "TEXT_OUTPUT_SELECT_ALL"
  }

  override var indexInResults = -1

  private val console = ColoredTextConsole(project, viewer = true)

  override val component: JComponent

  override val title
    get() = VisMessagesBundle.message("page.title.text")

  override val contentType = InlayPageContentType.TEXT

  init {
    Disposer.register(this, console)

    val consoleEditor = console.editor as EditorImpl

    if (!InlaysConfig.getInstance().transparentOutput) {
      consoleEditor.backgroundColor = UIUtil.getPanelBackground()
    }

    component = NotebookDataProvider.wrapComponent(console.component, editor.project, editor, cell.note, cell, hostEditor = consoleEditor)

    val actionSelect = object : AbstractAction(SELECT_ALL_ACTION_KEY) {
      override fun actionPerformed(e: ActionEvent) {
        consoleEditor.selectionModel.setSelection(0, console.text.length)
      }
    }
    consoleEditor.contentComponent.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                                                SELECT_ALL_ACTION_KEY)
    consoleEditor.contentComponent.actionMap.put(SELECT_ALL_ACTION_KEY, actionSelect)
  }

  override fun createActions(): List<AnAction> {
    val actionSaveAsTxt = DumbAwareAction.create(VisMessagesBundle.message("text.action.saveAs"), AllIcons.Actions.MenuSaveall) {
      saveAsTxt()
    }

    val actionWordWrap = object : DumbAwareToggleAction(VisMessagesBundle.message("text.action.softWrap"),
                                                        null, AllIcons.Actions.ToggleSoftWrap) {
      override fun isSelected(e: AnActionEvent) = console.editor?.softWrapModel?.isSoftWrappingEnabled ?: false

      // softWrapModel.isSoftWrappingEnabled should be called only from EDT
      override fun getActionUpdateThread() = ActionUpdateThread.EDT

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        console.editor?.let {
          it.settings.isUseSoftWraps = state
        }
      }
    }

    return listOf(actionSaveAsTxt, actionWordWrap)
  }

  private var appendNewLine = false

  fun clear() {
    appendNewLine = false
    console.clear()
  }

  fun addData(data: String) {
    val hasNewline = data.endsWith('\n')
    val trimmed = data.removeSuffix("\n")
    console.addData(if (appendNewLine) "\n$trimmed" else trimmed, ProcessOutputTypes.STDOUT)

    appendNewLine = hasNewline

    console.flushDeferredText()
    val editor = console.editor!!
    val preferredHeight = max(2, editor.document.lineCount) * editor.lineHeight + editor.lineHeight / 2
    console.scrollTo(0)
    component.preferredSize = Dimension(component.preferredSize.width, min(InlayDimensions.defaultHeight, preferredHeight))
  }

  // This method should not be called after addData() only after console.flushDeferredText() invoked.
  override fun getCollapsedDescription() = StringUtil.first(console.text, 80, true)

  private fun saveAsTxt() {
    val descriptor = FileSaverDescriptor(VisMessagesBundle.message("text.exportAs.text"),
                                         VisMessagesBundle.message("text.exportAs.hint"), "txt")
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val exportPath = InlaysSettings.getInstance().getTextExportPath(project)
    val fileWrapper = chooser.save(exportPath.parent, exportPath.name) ?: return
    InlaysSettings.getInstance().textExportPath = fileWrapper.file.path

    ApplicationManager.getApplication().runWriteAction {
      try {
        fileWrapper.file.bufferedWriter().use { out ->
          out.write(console.text)
        }
      }
      catch (e: Exception) {
        invokeLater {
          Messages.showErrorDialog(project, e.message, VisMessagesBundle.message("text.exportFailed"))
        }
      }
    }
  }
}