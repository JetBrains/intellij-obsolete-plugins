@file:JvmName("DbtModuleEntityModifications")

package com.intellij.dbt

import com.intellij.dbt.impl.DbtModuleEntityImpl
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface DbtModuleEntityBuilder : WorkspaceEntityBuilder<DbtModuleEntity> {
  override var entitySource: EntitySource
  var dbtProjectPath: VirtualFileUrl?
  var dbtExecutablePath: String?
  var dbtDataSourceId: String?
  var reviewed: Boolean
  var module: ModuleEntityBuilder
}

internal object DbtModuleEntityType : EntityType<DbtModuleEntity, DbtModuleEntityBuilder>() {
  override val entityImplClass: Class<*> get() = DbtModuleEntityImpl::class.java
  override val entityImplBuilderClass: Class<*> get() = DbtModuleEntityImpl.Builder::class.java
  operator fun invoke(
    reviewed: Boolean,
    entitySource: EntitySource,
    init: (DbtModuleEntityBuilder.() -> Unit)? = null,
  ): DbtModuleEntityBuilder {
    val builder = builder()
    builder.reviewed = reviewed
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyDbtModuleEntity(
  entity: DbtModuleEntity,
  modification: DbtModuleEntityBuilder.() -> Unit,
): DbtModuleEntity = modifyEntity(DbtModuleEntityBuilder::class.java, entity, modification)

var ModuleEntityBuilder.dbtSettings: DbtModuleEntityBuilder?
  by WorkspaceEntity.extensionBuilder(DbtModuleEntity::class.java)

@JvmOverloads
@JvmName("createDbtModuleEntity")
fun DbtModuleEntity(
  reviewed: Boolean,
  entitySource: EntitySource,
  init: (DbtModuleEntityBuilder.() -> Unit)? = null,
): DbtModuleEntityBuilder = DbtModuleEntityType(reviewed, entitySource, init)
