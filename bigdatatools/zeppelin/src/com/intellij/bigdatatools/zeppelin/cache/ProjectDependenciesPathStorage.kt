package com.intellij.bigdatatools.zeppelin.cache

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
  name = "ZeppelinDependencyPaths",
  storages = [Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)]
)
class ProjectDependenciesPathStorage : DependenciesPathStorage() {
  companion object {
    fun getInstance(project: Project) = project.getService(ProjectDependenciesPathStorage::class.java).state
  }
}