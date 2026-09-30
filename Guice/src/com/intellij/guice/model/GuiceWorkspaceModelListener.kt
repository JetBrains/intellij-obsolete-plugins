// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity

/**
 * Subscribes to [WorkspaceModel.eventLog][com.intellij.platform.backend.workspace.WorkspaceModel.eventLog]
 * and triggers a full rescan of the Guice index when the project structure
 * changes (libraries, modules, content roots, or source roots added/removed/modified).
 *
 * This ensures that the [GuiceNavigationIndex] is rebuilt with fresh PSI references
 * after dependency changes, which would otherwise leave stale
 * [SmartPsiElementPointer]s returning `null`.
 *
 * Registered as a `postStartupActivity` in `plugin.xml`.
 */
internal class GuiceWorkspaceModelListener : ProjectActivity {

  override suspend fun execute(project: Project) {
    val model = GuiceProjectModel.getInstance(project)
    model.backgroundUpdater.launchInScope {
      project.workspaceModel.eventLog.collect { event ->
        val hasLibraryChanges = event.getChanges(LibraryEntity::class.java).isNotEmpty()
        val hasModuleChanges = event.getChanges(ModuleEntity::class.java).isNotEmpty()
        val hasContentRootChanges = event.getChanges(ContentRootEntity::class.java).isNotEmpty()
        val hasSourceRootChanges = event.getChanges(SourceRootEntity::class.java).isNotEmpty()

        if (hasLibraryChanges || hasModuleChanges || hasContentRootChanges || hasSourceRootChanges) {
          model.markStructureChanged()
        }
      }
    }
  }
}
