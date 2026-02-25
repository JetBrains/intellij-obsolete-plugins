/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object TaglibArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "TagLib"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("taglib")

  override val annotationFqns: Collection<String> = listOf("grails.gsp.TagLib")

  override val icon: Icon = GroovyMvcIcons.Taglib

  override val title: String = "Tag Libraries"

  override val weight: Int = NodeWeights.TAGLIB_FOLDER
}