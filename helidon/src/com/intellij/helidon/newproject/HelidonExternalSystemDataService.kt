// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.newproject

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

internal class HelidonExternalSystemDataService : AbstractProjectDataService<LibraryDependencyData, Module>() {
  override fun getTargetDataKey(): Key<LibraryDependencyData> = ProjectKeys.LIBRARY_DEPENDENCY

  override fun onSuccessImport(imported: MutableCollection<DataNode<LibraryDependencyData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {
    // on import from Gradle
    project.getService(HelidonRunConfigurationService::class.java).createRunConfigurations(project)
  }
}