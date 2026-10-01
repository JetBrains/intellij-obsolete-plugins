@file:Suppress("DialogTitleCapitalization")

package com.intellij.dbt

import com.intellij.codeInsight.daemon.impl.EditorTrackerListener
import com.intellij.dbt.detection.DbtService
import com.intellij.dbt.diagram.openLineage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DbtProjectActivity(private val coroutineScope: CoroutineScope) : ProjectActivity {
  override suspend fun execute(project: Project) {
    subscribeToActiveEditor(project)

    val dbtService = DbtService.getInstance(project)
    project.modules.forEach {
      dbtService.processModule(it)
    }
  }

  private fun subscribeToActiveEditor(project: Project) {
    project.messageBus.connect().subscribe(EditorTrackerListener.TOPIC, object : EditorTrackerListener {
      override fun activeEditorsChanged(activeEditors: List<Editor>) {
        val editor = FileEditorManager.getInstance(project).selectedEditor
        if (editor != null) {
          val file = editor.file ?: return
          coroutineScope.launch {
            val module = ModuleUtilCore.findModuleForFile(file, project) ?: return@launch
            openLineage(editor.file, module)
          }
        }
      }
    })
  }
}