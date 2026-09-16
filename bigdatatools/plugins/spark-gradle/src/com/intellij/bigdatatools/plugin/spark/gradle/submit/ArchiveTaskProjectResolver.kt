package com.intellij.bigdatatools.plugin.spark.gradle.submit

import com.intellij.bigdatatools.plugin.spark.gradle.tooling.ArchiveTaskModel
import com.intellij.bigdatatools.plugin.spark.gradle.tooling.SparkToolingExtensionClass
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class ArchiveTaskProjectResolver : AbstractProjectResolverExtension() {

  override fun getExtraProjectModelClasses() = setOf(ArchiveTaskModel::class.java)

  override fun getToolingExtensionsClasses() = setOf(SparkToolingExtensionClass::class.java)

  override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
    val model = resolverCtx.getProjectModel(gradleModule, ArchiveTaskModel::class.java) ?: return
    for (taskNode in ExternalSystemApiUtil.getChildren(ideModule, ProjectKeys.TASK)) {
      taskNode.createChild(ArtifactTaskData.KEY, ArtifactTaskData(model.archiveTaskToArtifact[taskNode.data.name]))
    }
    super.populateModuleExtraModels(gradleModule, ideModule)
  }
}