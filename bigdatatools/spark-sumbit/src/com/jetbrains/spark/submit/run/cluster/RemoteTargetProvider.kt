package com.jetbrains.spark.submit.run.cluster

import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.util.InternalFeature

interface RemoteTargetProvider {
  suspend fun getTargetById(project: Project, id: RemoteTargetId): RemoteTarget?
  suspend fun getRemoteTargets(project: Project): List<RemoteTarget>

  fun createConnectionTarget(project: Project): AddConnectionOption?

  val connectionGroups: List<ConnectionGroup> get() = emptyList()

  companion object {
    private val EP_NAME = ExtensionPointName.create<RemoteTargetProvider>("com.intellij.bigdatatools.remote.target")

    private fun getAll() = EP_NAME.extensionList.filter { it !is InternalFeature || BdIdeRegistryUtil.isInternalFeaturesAvailable() }

    suspend fun getAllTargets(project: Project) = getAll().flatMap { it.getRemoteTargets(project) }
    fun getAllAddConnections(project: Project) = getAll().mapNotNull { it.createConnectionTarget(project) }
    suspend fun getTarget(project: Project, targetId: RemoteTargetId) = getAll().firstNotNullOfOrNull { it.getTargetById(project, targetId) }
    fun getConnectionGroups() = getAll().flatMap { it.connectionGroups }
  }

}