package com.intellij.bigdatatools.zeppelin.dependency.collector.user

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(
  name = "ZeppelinUserDeps",
  storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)]
)
class ZeppelinGlobalUserDependenciesStorage : ZeppelinUserDependenciesStorage()