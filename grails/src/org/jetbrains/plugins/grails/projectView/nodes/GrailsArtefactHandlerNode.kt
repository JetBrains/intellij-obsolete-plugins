/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler

class GrailsArtefactHandlerNode(
  project: Project,
  artefactHandler: GrailsDisplayableArtefactHandler,
  viewSettings: ViewSettings
) : GrailsArtefactHandlerNodeBase<GrailsDisplayableArtefactHandler>(project, viewSettings, artefactHandler) {

  override val artefactHandler: GrailsDisplayableArtefactHandler get() = value

  override fun update(presentation: PresentationData) {
    presentation.setIcon(value.groupIcon)
    presentation.presentableText = value.title
  }

  override fun getChildren(): TreeNodes = getArtefactNodes(project!!, settings, value, artefacts)

  override fun contains(file: VirtualFile): Boolean = file.isDirectory || mayContain(grailsApplication, file)
}
