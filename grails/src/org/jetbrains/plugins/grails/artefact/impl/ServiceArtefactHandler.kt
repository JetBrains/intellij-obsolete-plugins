/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object ServiceArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Service"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("services")

  override val icon: Icon = AllIcons.FileTypes.Config

  override val title: String = "Services"

  override val weight: Int = NodeWeights.SERVICES_FOLDER
}