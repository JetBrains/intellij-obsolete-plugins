package com.intellij.dbt

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

interface DbtModuleEntity : WorkspaceEntity {
  val dbtProjectPath: VirtualFileUrl?
  val dbtExecutablePath: String?
  val dbtDataSourceId: String?
  val reviewed: Boolean

  @Parent
  val module: ModuleEntity
}

val ModuleEntity.dbtSettings: DbtModuleEntity?
  by WorkspaceEntity.extension()


object DbtEntitySource : EntitySource