package com.intellij.bigdatatools.zeppelin.dependency.collector

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.BuiltinZeppelinDependencies
import com.intellij.bigdatatools.zeppelin.dependency.collector.interpreter.InterpreterDependenciesCollector
import com.intellij.bigdatatools.zeppelin.dependency.collector.user.ZeppelinUserDependenciesStorage
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibraryType
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class ZeppelinLibraryCollector(val project: Project?, val cachedConnection: ZeppelinInstanceCachedConnection) : Disposable {
  val config: ZeppelinConnectionData = cachedConnection.config

  override fun dispose() {}

  fun getAllRepositories() = cachedConnection.repositories + getInterpreterRepos()

  fun getAllDependencies() = getUserDependencies() + getInstanceDependencies() + getInterpreterDependencies()

  fun updateStoredLibs(libs: List<ZeppelinLibrary>) {
    val newUserDependencies = libs.filter { it.type == ZeppelinLibraryType.USER }
    updateUserDependencies(project, config.innerId, newUserDependencies.map { it.dep })
  }

  private fun getInterpreterRepos() = cachedConnection.interpreterSettings.flatMap {
    InterpreterDependenciesCollector.getConfRepositories(it)
  }

  private fun getInstanceDependencies(): List<ZeppelinLibrary> {
    val zepInfo: ZeppelinInfo = cachedConnection.zeppelinInfo ?: return emptyList()
    val dependencies = BuiltinZeppelinDependencies.getDepsForInstance(config, zepInfo)
    return dependencies.map { ZeppelinLibrary(it, ZeppelinLibraryType.BUILTIN) }
  }

  private fun getUserDependencies(): List<ZeppelinLibrary> {
    val deps = ZeppelinUserDependenciesStorage.getUserDependencies(project, config.innerId)
    return deps.map { ZeppelinLibrary(it, ZeppelinLibraryType.USER) }
  }

  private fun getInterpreterDependencies(): List<ZeppelinLibrary> {
    val interpreterSettings = cachedConnection.interpreterSettings
    return interpreterSettings.flatMap { InterpreterDependenciesCollector.collectDependencyFromInterpreterSettings(it) }
  }

  companion object {
    fun updateUserDependencies(project: Project?, configId: String, deps: List<ZepDependency>) {
      ZeppelinUserDependenciesStorage.replaceDependencies(project, configId, deps)
    }
  }
}