package com.intellij.bigdatatools.zeppelin.style

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteLineNumberingController
import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteLineWrapController
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.style.LinesNumberingMode
import com.intellij.bigdatatools.notebooks.style.NoteStyleSettings
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphFoldingController
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.bigdatatools.common.settings.wrappers.CheckBoxWrapper
import com.jetbrains.bigdatatools.common.settings.wrappers.ComboBoxWrapper
import javax.swing.JComponent
import javax.swing.JLabel

class ZeppelinStyleConfigurable : SearchableConfigurable {

  companion object {
    const val ID = "Notebook.Zeppelin.Style"
  }

  private val needConfirmCellDeletion = CheckBoxWrapper(JBCheckBox(ZepMessagesBundle.message("need.confirm.cell.delete")),
                                                        { NoteStyleSettings.getInstance().needConfirmCellDelete },
                                                        { value -> NoteStyleSettings.getInstance().needConfirmCellDelete = value }).apply {
    component.toolTipText = ZepMessagesBundle.message("need.confirm.cell.delete.hint")
  }

  private val needConfirmCellMerge = CheckBoxWrapper(JBCheckBox(ZepMessagesBundle.message("need.confirm.cell.merge")),
                                                     { NoteStyleSettings.getInstance().needConfirmCellMerge },
                                                     { value -> NoteStyleSettings.getInstance().needConfirmCellMerge = value }).apply {
    component.toolTipText = ZepMessagesBundle.message("need.confirm.cell.merge.hint")
  }

  private val needConfirmCellSplit = CheckBoxWrapper(JBCheckBox(ZepMessagesBundle.message("need.confirm.cell.split")),
                                                     { NoteStyleSettings.getInstance().needConfirmCellSplit },
                                                     { value -> NoteStyleSettings.getInstance().needConfirmCellSplit = value }).apply {
    component.toolTipText = ZepMessagesBundle.message("need.confirm.cell.split.hint")
  }

  private val cellsFolding = CheckBoxWrapper(JBCheckBox(ZepMessagesBundle.message("style.cellsFolding")),
                                             { ZeppelinStyleSettings.getInstance().cellsFolding },
                                             { value -> ZeppelinStyleSettings.getInstance().cellsFolding = value }).apply {
    component.toolTipText = ZepMessagesBundle.message("style.cellsFolding.hint")
  }

  private val linesNumbering = ComboBoxWrapper(ComboBox(LinesNumberingMode.entries.toTypedArray())
                                                 .apply { renderer = CustomListCellRenderer<LinesNumberingMode> { it.title } },
                                               { NoteStyleSettings.getInstance().linesNumbering },
                                               { value -> NoteStyleSettings.getInstance().linesNumbering = value }).apply {
    label = JLabel(ZepMessagesBundle.message("style.linesNumbering")).apply {
      toolTipText = ZepMessagesBundle.message("style.linesNumbering.hint")
    }
    component.toolTipText = ZepMessagesBundle.message("style.linesNumbering.hint")
  }

  private val editorSoftWraps = CheckBoxWrapper(JBCheckBox(ZepMessagesBundle.message("style.editorSoftWraps")),
                                                { NoteStyleSettings.getInstance().editorSoftWraps },
                                                { value -> NoteStyleSettings.getInstance().editorSoftWraps = value }).apply {
    component.toolTipText = ZepMessagesBundle.message("style.editorSoftWraps.hint")
  }

  private val componentsList = arrayOf(cellsFolding,
                                       linesNumbering,
                                       editorSoftWraps,
                                       needConfirmCellDeletion,
                                       needConfirmCellMerge,
                                       needConfirmCellSplit)

  override fun reset() = componentsList.forEach { it.reset() }

  override fun apply() {

    val linesNumberingModified = linesNumbering.isModified()
    val cellsFoldingModified = cellsFolding.isModified()
    val softWrapsModified = editorSoftWraps.isModified()

    componentsList.forEach { it.apply() }

    EditorFactory.getInstance().allEditors.forEach {
      val virtualFile = FileDocumentManager.getInstance().getFile(it.document) as? NotebookVirtualFile ?: return@forEach
      if (linesNumberingModified) {
        NoteLineNumberingController.update(it, virtualFile.notebook)
      }
      if (cellsFoldingModified) {
        ZeppelinParagraphFoldingController.update(it, virtualFile.notebook)
      }
      if (softWrapsModified) {
        NoteLineWrapController.updateEditor(it)
      }
    }
  }

  override fun isModified() = componentsList.firstOrNull { it.isModified() } != null

  override fun createComponent(): JComponent {
    return MigPanel().apply {
      componentsList.forEach {
        val label = it.label
        if (label == null) {
          add(it.component, UiUtil.spanXWrap)
        }
        else {
          shortRow(label, it.component)
        }
      }
    }
  }

  override fun getDisplayName() = ZepMessagesBundle.message("style.displayName")

  override fun getId() = ID

  override fun disposeUIResources() = Unit
}