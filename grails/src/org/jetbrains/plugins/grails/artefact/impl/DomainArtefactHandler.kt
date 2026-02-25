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

object DomainArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Domain"

  override val artefactClassSuffix: String = ""

  override val annotationFqns: Collection<String> = listOf("grails.persistence.Entity", "grails.gorm.annotation.Entity")

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("domain")

  override val icon: Icon = AllIcons.Nodes.DataTables

  override val groupIcon: Icon = AllIcons.Nodes.Models

  override val title: String = "Domain Classes"

  override val weight: Int = NodeWeights.DOMAIN_CLASSES_FOLDER
}