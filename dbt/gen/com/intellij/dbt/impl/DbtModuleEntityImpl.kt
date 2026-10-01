@file:OptIn(EntityStorageInstrumentationApi::class)

package com.intellij.dbt.impl

import com.intellij.dbt.DbtModuleEntity
import com.intellij.dbt.DbtModuleEntityBuilder
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.impl.EntityLink
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.instrumentation
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class DbtModuleEntityImpl(private val dataSource: DbtModuleEntityData) : DbtModuleEntity, WorkspaceEntityBase(dataSource) {
  private companion object {
    internal val MODULE_CONNECTION_ID: ConnectionId =
      ConnectionId.create(ModuleEntity::class.java, DbtModuleEntity::class.java, ConnectionId.ConnectionType.ONE_TO_ONE, false)
    private val connections = listOf<ConnectionId>(MODULE_CONNECTION_ID)
  }

  override val dbtProjectPath: VirtualFileUrl?
    get() {
      readField("dbtProjectPath")
      return dataSource.dbtProjectPath
    }
  override val dbtExecutablePath: String?
    get() {
      readField("dbtExecutablePath")
      return dataSource.dbtExecutablePath
    }
  override val dbtDataSourceId: String?
    get() {
      readField("dbtDataSourceId")
      return dataSource.dbtDataSourceId
    }
  override val reviewed: Boolean
    get() {
      readField("reviewed")
      return dataSource.reviewed
    }
  override val module: ModuleEntity
    get() = snapshot.instrumentation.getParent(MODULE_CONNECTION_ID, this) as? ModuleEntity
            ?: error("Parent module not found for DbtModuleEntity")
  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }

  internal class Builder(result: DbtModuleEntityData?) : ModifiableWorkspaceEntityBase<DbtModuleEntity, DbtModuleEntityData>(result),
                                                         DbtModuleEntityBuilder {
    internal constructor() : this(DbtModuleEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity DbtModuleEntity is already created in a different builder")
        }
      }
      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
// After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
// Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null
      index(this, "dbtProjectPath", this.dbtProjectPath)
// Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization()
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (_diff != null) {
        if (_diff.instrumentation.getParentBuilder(MODULE_CONNECTION_ID, this) == null) {
          error("Field DbtModuleEntity#module should be initialized")
        }
      }
      else {
        if (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] == null) {
          error("Field DbtModuleEntity#module should be initialized")
        }
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as DbtModuleEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.dbtProjectPath != dataSource?.dbtProjectPath) this.dbtProjectPath = dataSource.dbtProjectPath
      if (this.dbtExecutablePath != dataSource?.dbtExecutablePath) this.dbtExecutablePath = dataSource.dbtExecutablePath
      if (this.dbtDataSourceId != dataSource?.dbtDataSourceId) this.dbtDataSourceId = dataSource.dbtDataSourceId
      if (this.reviewed != dataSource.reviewed) this.reviewed = dataSource.reviewed
      updateChildToParentReferences(parents)
    }

    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")
      }
    override var dbtProjectPath: VirtualFileUrl?
      get() = getEntityData().dbtProjectPath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).dbtProjectPath = value
        changedProperty.add("dbtProjectPath")
        val _diff = diff
        if (_diff != null) index(this, "dbtProjectPath", value)
      }
    override var dbtExecutablePath: String?
      get() = getEntityData().dbtExecutablePath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).dbtExecutablePath = value
        changedProperty.add("dbtExecutablePath")
      }
    override var dbtDataSourceId: String?
      get() = getEntityData().dbtDataSourceId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).dbtDataSourceId = value
        changedProperty.add("dbtDataSourceId")
      }
    override var reviewed: Boolean
      get() = getEntityData().reviewed
      set(value) {
        checkModificationAllowed()
        getEntityData(true).reviewed = value
        changedProperty.add("reviewed")
      }
    override var module: ModuleEntityBuilder
      get() {
        val _diff = diff
        return if (_diff != null) {
          ((_diff as MutableEntityStorageInstrumentation).getParentBuilder(MODULE_CONNECTION_ID, this) as? ModuleEntityBuilder)
          ?: (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] as? ModuleEntityBuilder)
          ?: error("module is null for DbtModuleEntity")
        }
        else {
          (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] as? ModuleEntityBuilder) ?: error("module is null for DbtModuleEntity")
        }
      }
      set(value) {
        checkModificationAllowed()
        val _diff = diff
        if (_diff != null && value is ModifiableWorkspaceEntityBase<*, *> && value.diff == null) {
          value.entityLinks[EntityLink(true, MODULE_CONNECTION_ID)] = this
          @Suppress("UNCHECKED_CAST")
          _diff.addEntity(value as ModifiableWorkspaceEntityBase<WorkspaceEntity, *>)
        }
        if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*, *> || value.diff != null)) {
          _diff.instrumentation.addChild(MODULE_CONNECTION_ID, value, this)
        }
        else {
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            value.entityLinks[EntityLink(true, MODULE_CONNECTION_ID)] = this
          }
          this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] = value
        }
        changedProperty.add("module")
      }

    override fun getEntityClass(): Class<DbtModuleEntity> = DbtModuleEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class DbtModuleEntityData : WorkspaceEntityData<DbtModuleEntity>() {
  var dbtProjectPath: VirtualFileUrl? = null
  var dbtExecutablePath: String? = null
  var dbtDataSourceId: String? = null
  var reviewed: Boolean = false
  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntityBuilder<DbtModuleEntity> {
    val modifiable = DbtModuleEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorageInstrumentation): DbtModuleEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = DbtModuleEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("com.intellij.dbt.DbtModuleEntity") as EntityMetadata
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return DbtModuleEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*> {
    return DbtModuleEntity(reviewed, entitySource) {
      this.dbtProjectPath = this@DbtModuleEntityData.dbtProjectPath
      this.dbtExecutablePath = this@DbtModuleEntityData.dbtExecutablePath
      this.dbtDataSourceId = this@DbtModuleEntityData.dbtDataSourceId
      parents.filterIsInstance<ModuleEntityBuilder>().singleOrNull()?.let { this.module = it }
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    res.add(ModuleEntity::class.java)
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as DbtModuleEntityData
    if (this.entitySource != other.entitySource) return false
    if (this.dbtProjectPath != other.dbtProjectPath) return false
    if (this.dbtExecutablePath != other.dbtExecutablePath) return false
    if (this.dbtDataSourceId != other.dbtDataSourceId) return false
    if (this.reviewed != other.reviewed) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as DbtModuleEntityData
    if (this.dbtProjectPath != other.dbtProjectPath) return false
    if (this.dbtExecutablePath != other.dbtExecutablePath) return false
    if (this.dbtDataSourceId != other.dbtDataSourceId) return false
    if (this.reviewed != other.reviewed) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + dbtProjectPath.hashCode()
    result = 31 * result + dbtExecutablePath.hashCode()
    result = 31 * result + dbtDataSourceId.hashCode()
    result = 31 * result + reviewed.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + dbtProjectPath.hashCode()
    result = 31 * result + dbtExecutablePath.hashCode()
    result = 31 * result + dbtDataSourceId.hashCode()
    result = 31 * result + reviewed.hashCode()
    return result
  }
}
