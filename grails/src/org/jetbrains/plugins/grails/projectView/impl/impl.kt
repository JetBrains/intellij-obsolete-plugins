/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.icons.AllIcons
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.artefact.impl.getArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler as ArtefactHandler

internal fun shouldShowItem(item: PsiFileSystemItem): Boolean {
  if (item !is GroovyFile) return true
  val clazz = item.classes.singleOrNull() ?: return true
  val handler = getArtefactHandler(clazz)
  return handler !is ArtefactHandler
}

internal val specialGrailsAppFolders = mapOf(
  "conf" to Triple(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER, "Configuration"),
  "views" to Triple(GroovyMvcIcons.Gsp_logo, NodeWeights.VIEWS_FOLDER, "Views"),
  "init" to Triple(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER - 1, "Initialization")
)

internal fun GrailsApplication.findPsiFile(name: String) = root.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findFile(it)
}

internal fun GrailsApplication.findPsiDirectory(name: String) = root.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findDirectory(it)
}

internal fun GrailsApplication.findAppPsiDirectory(name: String) = appRoot.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findDirectory(it)
}
