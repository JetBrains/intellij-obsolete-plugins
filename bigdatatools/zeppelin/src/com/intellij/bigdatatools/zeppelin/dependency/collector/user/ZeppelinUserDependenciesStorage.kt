package com.intellij.bigdatatools.zeppelin.dependency.collector.user

import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.xmlb.XmlSerializerUtil

open class ZeppelinUserDependenciesStorage : PersistentStateComponent<ZeppelinUserDependenciesStorage> {
  @Suppress("MemberVisibilityCanBePrivate")
  var userDependenciesStrings: MutableMap<String, List<String>> = mutableMapOf()

  override fun getState() = this
  override fun loadState(state: ZeppelinUserDependenciesStorage) = XmlSerializerUtil.copyBean(state, this)

  private fun getDepsForConfig(id: String): List<ZepDependency> {
    val rawDeps = userDependenciesStrings[id] ?: emptyList()
    return rawDeps.map { BdtJson.fromJsonToClass(it, ZepDependency::class.java) }
  }

  private fun replaceDeps(id: String, deps: List<ZepDependency>) {
    val jsonDeps = deps.map {
      JsonParser.toJson(it)
    }
    val state = state
    state.userDependenciesStrings[id] = jsonDeps
  }

  private fun clearDeps(id: String) {
    userDependenciesStrings.remove(id)
  }

  companion object {
    fun getUserDependencies(project: Project?, configId: String) = if (project != null) {
      val projectDeps = getInstance(project).getDepsForConfig(configId)
      if (projectDeps.isNotEmpty())
        projectDeps
      else
        getInstance(null).getDepsForConfig(configId)
    }
    else {
      val globalDeps = getInstance(null).getDepsForConfig(configId)
      if (globalDeps.isNotEmpty()) {
        globalDeps
      }
      else {
        ProjectManager.getInstance().openProjects.flatMap {
          getInstance(it).getDepsForConfig(configId)
        }.distinct()
      }
    }

    fun clearDependencies(configId: String) {
      ProjectManager.getInstance().openProjects.forEach {
        getInstance(it).clearDeps(configId)
      }
      getInstance(null).clearDeps(configId)
    }

    fun replaceDependencies(project: Project?, id: String, deps: List<ZepDependency>) {
      if (project == null) {
        ProjectManager.getInstance().openProjects.forEach {
          getInstance(it).clearDeps(id)
        }
        getInstance(null).replaceDeps(id, deps)
      }
      else {
        getInstance(null).clearDeps(id)
        getInstance(project).replaceDeps(id, deps)
      }
    }

    private fun getInstance(project: Project?): ZeppelinUserDependenciesStorage = if (project != null)
      project.getService(ZeppelinProjectUserDependenciesStorage::class.java)
    else
      ApplicationManager.getApplication().getService(ZeppelinGlobalUserDependenciesStorage::class.java)
  }
}