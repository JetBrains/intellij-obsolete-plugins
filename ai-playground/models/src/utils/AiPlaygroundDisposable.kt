package com.intellij.aiplayground.models.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project


/**
 * The service is intended to be used instead of a project/application as a parent disposable.
 */
@Service(Service.Level.APP, Service.Level.PROJECT)
class AiPlaygroundPluginDisposable : Disposable {
  override fun dispose() {
  }

  companion object {
    val instance: Disposable
      get() = ApplicationManager.getApplication().getService<AiPlaygroundPluginDisposable>(AiPlaygroundPluginDisposable::class.java)

    fun getInstance(project: Project): Disposable {
      return project.getService(AiPlaygroundPluginDisposable::class.java)
    }
  }
}