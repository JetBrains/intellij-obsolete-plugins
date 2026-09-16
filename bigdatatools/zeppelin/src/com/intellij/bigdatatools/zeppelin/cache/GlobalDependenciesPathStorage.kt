package com.intellij.bigdatatools.zeppelin.cache

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(
  name = "ZeppelinGlobDependencyPaths",
  storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)]
)
class GlobalDependenciesPathStorage : DependenciesPathStorage() {
  companion object {
    fun getInstance() = ApplicationManager.getApplication().getService(GlobalDependenciesPathStorage::class.java).state
  }
}