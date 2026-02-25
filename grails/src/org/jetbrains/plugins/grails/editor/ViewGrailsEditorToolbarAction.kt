/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.editor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.ui.EditorNotifications

class ViewGrailsEditorToolbarAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isVisible = isVisible(e)
  }

  private fun isVisible(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    val fileEditor = e.getData(PlatformCoreDataKeys.FILE_EDITOR) ?: return false
    val file = fileEditor.file ?: return false
    return shouldBeDecorated(file, fileEditor, project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return showEditorToolBar
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    showEditorToolBar = state
    val project = e.project ?: return
    EditorNotifications.getInstance(project).updateAllNotifications()
  }
}