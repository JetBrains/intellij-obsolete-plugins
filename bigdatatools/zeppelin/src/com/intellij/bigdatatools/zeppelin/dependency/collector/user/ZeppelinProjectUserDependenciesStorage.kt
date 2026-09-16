package com.intellij.bigdatatools.zeppelin.dependency.collector.user

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(
  name = "ZeppelinUserDeps",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ZeppelinProjectUserDependenciesStorage : ZeppelinUserDependenciesStorage()