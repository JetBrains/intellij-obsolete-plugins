/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.getArtefacts
import org.jetbrains.plugins.grails.structure.GrailsApplication

abstract class GrailsArtefactHandlerNodeBase<T : Any>(
  project: Project,
  viewSettings: ViewSettings,
  value: T
) : ProjectViewNode<T>(project, value, viewSettings) {

  val grailsApplication: GrailsApplication get() = findNotNullValueOfType()
  protected val scope: GlobalSearchScope get() = grailsApplication.getScope(includeDependencies = false, testsOnly = false)
  protected abstract val artefactHandler: GrailsDisplayableArtefactHandler
  protected val artefacts: Classes get() = artefactHandler.getArtefacts(grailsApplication, scope)
}
