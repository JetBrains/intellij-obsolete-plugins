package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.components

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.BigPopupUI
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class GoToParagraphBigPopUp(project: Project,
                            val note: ZeppelinNotebook,
                            val editor: Editor) : BigPopupUI(project) {

  val model = CollectionListModel<ParagraphItem>()
  val searchList: JBList<ParagraphItem> = JBList(model)

  init {
    init()
    updateViewType(ViewType.FULL)

    DumbAwareAction.create { onSelect() }
      .registerCustomShortcutSet(
        CustomShortcutSet.fromString("ENTER", "shift ENTER", "alt ENTER", "alt shift ENTER", "meta ENTER"), mySearchField, this)

    val escape = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE)
    DumbAwareAction.create {
      searchFinishedHandler.run()
    }.registerCustomShortcutSet(escape?.shortcutSet ?: CommonShortcuts.ESCAPE, this)

    myResultsList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val i = myResultsList.locationToIndex(e.point)
        if (i == -1) return
        searchList.selectedIndex = i
        onSelect()
      }
    })

    // To add search icon to the beginning of text filed.
    mySearchField.addSearchExtension()

    rebuildList()
    mySearchField.document.addUndoableEditListener { rebuildList() }
    searchList.setEmptyText(ZepMessagesBundle.message("go.to.paragraph.empty.text"))
  }

  override fun getInitialHint(): String = IdeBundle.message("run.anything.hint.initial.text",
                                                            KeymapUtil.getKeystrokeText(RunAnythingPopupUI.UP_KEYSTROKE),
                                                            KeymapUtil.getKeystrokeText(RunAnythingPopupUI.DOWN_KEYSTROKE))

  override fun dispose() {}

  @Suppress("UNCHECKED_CAST")
  override fun createList() = searchList as JBList<Any>

  override fun createCellRenderer(): ListCellRenderer<Any> = GoToParagraphCellRender()

  override fun createHeader(): JComponent {
    val titleField = JLabel(ZepMessagesBundle.message("action.GotoParagraph.text")).apply {
      border = BorderFactory.createEmptyBorder(3, 5, 5, 0)

      font = if (SystemInfo.isMac)
        font.deriveFont(Font.BOLD, font.size - 1f)
      else
        font.deriveFont(Font.BOLD)
    }

    return JPanel(BorderLayout()).apply {
      add(titleField)
    }
  }

  override fun getAccessibleName(): String = ZepMessagesBundle.message("action.GotoParagraph.text")

  fun getSelectedItem() = myResultsList.selectedValue as? ParagraphItem

  private fun onSelect() {
    val index = myResultsList.selectedIndex
    if (index == -1) return
    val selectedValue = myResultsList.selectedValue as ParagraphItem
    searchFinishedHandler.run()
    invokeLater {
      NotebookEditorUtils.goToCell(editor, selectedValue.cell)
    }
  }

  private fun rebuildList() {
    val searchText = mySearchField.text
    val items = note.cells.map { ParagraphItem(it, editor) }.filter { it.title.contains(searchText) }
    model.replaceAll(items)
    if (items.isNotEmpty())
      searchList.selectedIndex = 0
  }
}