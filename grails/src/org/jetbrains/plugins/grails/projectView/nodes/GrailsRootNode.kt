/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

class GrailsRootNode(project: Project, viewSettings: ViewSettings) : ProjectViewNode<Project>(project, project, viewSettings) {

  override fun getChildren(): List<GrailsApplicationNode> = GrailsApplicationManager.getInstance(value).applications.map {
    GrailsApplicationNode(it, settings)
  }

  override fun update(presentation: PresentationData): Unit = Unit

  override fun contains(file: VirtualFile): Boolean = GrailsApplicationManager.getInstance(value).findApplication(file) != null
}
