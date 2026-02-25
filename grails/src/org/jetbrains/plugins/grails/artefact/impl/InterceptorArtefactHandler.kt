/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.Grails3Application
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object InterceptorArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Interceptor"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("controllers")

  override val icon: Icon = AllIcons.General.Filter

  override val title: String get() = "Interceptors"

  override val weight: Int get() = NodeWeights.CONTROLLERS_FOLDER + 1

  override fun isVisible(application: GrailsApplication): Boolean = application is Grails3Application
}