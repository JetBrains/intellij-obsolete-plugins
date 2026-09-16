package com.intellij.bigdatatools.zeppelin.cache

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.XmlSerializerUtil

open class DependenciesPathStorage : PersistentStateComponent<DependenciesPathStorage> {
  @Suppress("MemberVisibilityCanBePrivate")
  var globalLibrariesPathsMap: Map<String, List<String>> = mapOf()

  @Suppress("MemberVisibilityCanBePrivate")
  var configLibrariesPaths: Map<String, Map<String, List<String>>> = mapOf()

  override fun getState() = this
  override fun loadState(state: DependenciesPathStorage) = XmlSerializerUtil.copyBean(state, this)

  fun getGlobalLibraryPaths(libraryName: String) = globalLibrariesPathsMap[libraryName]

  fun clearForId(id: String) {
    val mutableMap = configLibrariesPaths.toMutableMap()
    mutableMap.remove(id)
    val configs = mutableMap.toMap()
    configLibrariesPaths = configs

  }

  fun getConnectionDepsPaths(configId: String, libraryName: String): List<String>? =
    configLibrariesPaths[configId]?.get(libraryName)

  fun saveGlobalLibraryPaths(libraryName: String, paths: List<String>) {
    globalLibrariesPathsMap = globalLibrariesPathsMap + mapOf(libraryName to paths)
  }

  fun saveInterpreterLibraryPaths(configId: String, libraryName: String, urls: List<String>) {
    val oldMap = configLibrariesPaths[configId] ?: emptyMap()
    val configLibraries = oldMap + mapOf(libraryName to urls)

    configLibrariesPaths = configLibrariesPaths + mapOf(configId to configLibraries)
  }

  fun clearAll() {
    configLibrariesPaths = emptyMap()
    globalLibrariesPathsMap = emptyMap()
  }
}