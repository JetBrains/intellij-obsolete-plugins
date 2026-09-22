// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model

import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import kotlinx.coroutines.flow.collectIndexed

/**
 * Subscribes to [WorkspaceModel.eventLog][com.intellij.platform.backend.workspace.WorkspaceModel.eventLog]
 * and triggers a full rescan of the Guice index when the project structure
 * changes (libraries or modules added/removed/modified).
 *
 * This ensures that the [GuiceLiveIndex] is rebuilt with fresh PSI references
 * after dependency changes, which would otherwise leave stale
 * [SmartPsiElementPointer]s returning `null`.
 *
 * Registered as a `postStartupActivity` in `plugin.xml`.
 */
internal class GuiceWorkspaceModelListener : ProjectActivity {

  override suspend fun execute(project: Project) {
    project.workspaceModel.eventLog.collectIndexed { index, event ->
      // Skip the initial snapshot (index == 0) — the index is built lazily
      // on first getIndex() call anyway.
      if (index == 0) return@collectIndexed

      val hasLibraryChanges = event.getChanges(LibraryEntity::class.java).isNotEmpty()
      val hasModuleChanges = event.getChanges(ModuleEntity::class.java).isNotEmpty()

      if (hasLibraryChanges || hasModuleChanges) {
        GuiceProjectModel.getInstance(project).markStructureChanged()
      }
    }
  }
}
