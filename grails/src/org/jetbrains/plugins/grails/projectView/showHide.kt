/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.grails.config.GrailsConstants
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import java.util.function.Predicate

internal const val ID = GrailsConstants.GRAILS

internal fun showHide(project: Project) {
  val grailsApplicationManager = GrailsApplicationManager.getInstance(project)
  val projectView = ProjectView.getInstance(project)
  if (grailsApplicationManager.hasApplications()) {
    if (ID !in projectView.paneIds) {
      projectView.addProjectPane(getPane(project))
    }
  }
  else if (ID in projectView.paneIds) {
    projectView.removeProjectPane(getPane(project))
  }
}

private fun getPane(project: Project) = AbstractProjectViewPane.EP.findFirstSafe(project, Predicate { it.id == ID })!!