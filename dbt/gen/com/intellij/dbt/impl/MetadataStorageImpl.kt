package com.intellij.dbt.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.metadata.model.ExtPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

@OptIn(WorkspaceEntityInternalApi::class)
internal object MetadataStorageImpl : MetadataStorageBase() {
  override fun initializeMetadata() {
    val primitiveTypeStringNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = true, type = "String")
    val primitiveTypeBooleanNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Boolean")
    var typeMetadata: StorageTypeMetadata
    typeMetadata = FinalClassMetadata.ObjectMetadata(fqName = "com.intellij.dbt.DbtEntitySource",
                                                     properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                                             isKey = false,
                                                                                             isOpen = false,
                                                                                             name = "virtualFileUrl",
                                                                                             valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                               isNullable = true,
                                                                                               typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                 fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                                             withDefault = false)),
                                                     supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource"))
    addMetadata(typeMetadata)
    typeMetadata = EntityMetadata(fqName = "com.intellij.dbt.DbtModuleEntity",
                                  entityDataFqName = "com.intellij.dbt.impl.DbtModuleEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "dbtProjectPath",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "dbtExecutablePath",
                                                                          valueType = primitiveTypeStringNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "dbtDataSourceId",
                                                                          valueType = primitiveTypeStringNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "reviewed",
                                                                          valueType = primitiveTypeBooleanNotNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "module",
                                                                          valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                        entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                                                                        isChild = false,
                                                                                                                        isNullable = false),
                                                                          withDefault = false)),
                                  extProperties = listOf(ExtPropertyMetadata(isComputable = false,
                                                                             isOpen = false,
                                                                             name = "dbtSettings",
                                                                             receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                             valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                           entityFqName = "com.intellij.dbt.DbtModuleEntity",
                                                                                                                           isChild = true,
                                                                                                                           isNullable = true),
                                                                             withDefault = false)),
                                  isAbstract = false)
    addMetadata(typeMetadata)
  }

  override fun initializeMetadataHash() {
    addMetadataHash(typeFqn = "com.intellij.dbt.DbtModuleEntity", metadataHash = -1635615691)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.EntitySource", metadataHash = 864461162)
    addMetadataHash(typeFqn = "com.intellij.dbt.DbtEntitySource", metadataHash = 1243007296)
  }
}
