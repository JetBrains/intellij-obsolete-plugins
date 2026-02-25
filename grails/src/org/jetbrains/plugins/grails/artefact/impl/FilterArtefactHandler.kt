/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler
import org.jetbrains.plugins.grails.artefact.api.IconOwner
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object FilterArtefactHandler : GrailsArtefactHandler, IconOwner {

  override val artefactHandlerID: String = "Filters"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("conf")

  override val icon: Icon = AllIcons.General.Filter

}
