package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

internal class DatabricksSyncManager(private val dataManager: DatabricksDataManager) : Disposable {
  private val tasks = mutableMapOf<Project, DatabricksSyncProjectTask>()

  override fun dispose() {}

  fun getTaskForProject(project: Project) = tasks.getOrPut(project) {
    val task = DatabricksSyncProjectTask(project, dataManager)
    Disposer.register(this, task)
    @Suppress("IncorrectParentDisposable")
    Disposer.register(project, task)
    @Suppress("IncorrectParentDisposable")
    Disposer.register(project) {
      tasks.remove(project)
    }

    task
  }

  companion object
}