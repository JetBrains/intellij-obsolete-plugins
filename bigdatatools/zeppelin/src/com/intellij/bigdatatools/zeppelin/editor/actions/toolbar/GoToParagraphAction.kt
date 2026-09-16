// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataKeys
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinEditorAction
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.components.GoToParagraphBigPopUp
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.components.ParagraphItem
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.lightEdit.LightEditCompatible
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.WindowStateService
import com.intellij.util.ui.JBUI

// BdtFullTextSearchAction is nearly clone of this.
class GoToParagraphAction : AnAction(), DumbAware, ZeppelinEditorAction, LightEditCompatible {

  private lateinit var goToParagraphView: GoToParagraphBigPopUp
  private lateinit var myBalloon: JBPopup

  private var mySelectedItem: ParagraphItem? = null

  init {
    shortcutSet = CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts("GotoLine"))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.noteEditor ?: return
    val notebook = e.getData(NotebookDataKeys.NOTE) ?: (e.getData(CommonDataKeys.VIRTUAL_FILE) as? NotebookVirtualFile)?.notebook ?: return

    IdeEventQueue.getInstance().popupManager.closeAllPopups(false)

    goToParagraphView = GoToParagraphBigPopUp(project, notebook as ZeppelinNotebook, editor)
    goToParagraphView.setSearchFinishedHandler {
      if (isShown()) {
        myBalloon.cancel()
      }
    }

    predefineSelectedItem()

    myBalloon = JBPopupFactory.getInstance().createComponentPopupBuilder(goToParagraphView, goToParagraphView.searchField)
      .setProject(project)
      .setModalContext(false)
      .setCancelOnClickOutside(true)
      .setRequestFocus(true)
      .setCancelKeyEnabled(true)
      .setCancelCallback {
        if (isShown() && goToParagraphView.getSelectedItem() != mySelectedItem) {
          saveSearchedItem()
        }
        true
      }
      .setResizable(true)
      .setMovable(true)
      .setDimensionServiceKey(project, LOCATION_SETTINGS_KEY, true)
      .setLocateWithinScreenBounds(false)
      .createPopup()

    Disposer.register(editor.disposable, myBalloon)

    val minimumSize = JBUI.size(250, 120)
    goToParagraphView.preferredSize = if (WindowStateService.getInstance(project).getSize(LOCATION_SETTINGS_KEY) == null) {
      JBUI.size(300, 400)
    }
    else {
      minimumSize
    }
    myBalloon.setMinimumSize(minimumSize)

    Disposer.register(myBalloon, goToParagraphView)

    calcPositionAndShow(project, myBalloon)
  }

  override fun update(e: AnActionEvent) {
    val editor = e.noteEditor
    val virtualFile = e.getData(NotebookDataKeys.NOTE) ?: (e.getData(CommonDataKeys.VIRTUAL_FILE) as? NotebookVirtualFile)
    e.presentation.isEnabledAndVisible = editor != null && virtualFile != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun predefineSelectedItem() {
    if (mySelectedItem != null) {
      val index = goToParagraphView.model.getElementIndex(mySelectedItem)
      goToParagraphView.searchList.selectedIndex = index
      goToParagraphView.searchList.ensureIndexIsVisible(index)
    }
  }

  private fun saveSearchedItem() {
    mySelectedItem = goToParagraphView.getSelectedItem()
  }

  private fun calcPositionAndShow(project: Project, balloon: JBPopup) {
    balloon.showCenteredInCurrentWindow(project)
  }

  private fun isShown(): Boolean = !myBalloon.isDisposed

  companion object {
    private const val LOCATION_SETTINGS_KEY = "goto.paragraph.popup"
  }
}