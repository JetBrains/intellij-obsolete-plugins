/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.projectView.nodes.GrailsControllerNode
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object ControllerArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Controller"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("controllers")

  override val annotationFqns: Collection<String> get() = listOf("grails.web.Controller")

  override val icon: Icon = AllIcons.Nodes.Controller

  override val groupIcon: Icon get() = AllIcons.Nodes.KeymapTools

  override val title: String = "Controllers"

  override val weight: Int = NodeWeights.CONTROLLERS_FOLDER

  override fun createNode(artefact: PsiClass, settings: ViewSettings): GrailsControllerNode = GrailsControllerNode(artefact, settings)
}