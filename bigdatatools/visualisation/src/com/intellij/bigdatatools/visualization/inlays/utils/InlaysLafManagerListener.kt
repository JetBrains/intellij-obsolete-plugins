package com.intellij.bigdatatools.visualization.inlays.utils

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.visualization.inlays.InlaysManagerImpl
import com.intellij.diagnostic.LoadingState
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.bigdatatools.common.util.invokeLater

class InlaysLafManagerListener : LafManagerListener {
  override fun lookAndFeelChanged(source: LafManager) {
    if (!LoadingState.APP_STARTED.isOccurred) { // To prevent useless run on first event on App initialization.
      return
    }
    //We need to run
    invokeLater {
      ProjectManager.getInstance().openProjects.forEach { project ->
        FileEditorManager.getInstance(project).allEditors.forEach { editor ->
          (editor as? NotebookEditor)?.editor?.getUserData(
            InlaysManagerImpl.EDITOR_INLAYS_MANAGER_KEY)?.inlays?.forEach {
            it.value.updateSelectionHighlighter(force = true)
          }
        }
      }
    }
  }
}