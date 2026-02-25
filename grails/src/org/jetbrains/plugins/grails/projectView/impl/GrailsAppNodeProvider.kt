/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.artefact.api.displayableArtefactHandlers
import org.jetbrains.plugins.grails.projectView.api.GrailsViewNodeProvider
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactHandlerNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.projectView.nodes.OtherGrailsAppSourcesNode
import org.jetbrains.plugins.grails.structure.GrailsApplication

internal class GrailsAppNodeProvider : GrailsViewNodeProvider {
  override fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    val project = application.project

    val result = mutableListOf<AbstractTreeNode<*>>()

    displayableArtefactHandlers.filter { it.isVisible(application) }.mapTo(result) {
      GrailsArtefactHandlerNode(project, it, settings)
    }

    for ((name, data) in specialGrailsAppFolders) {
      application.findAppPsiDirectory(name)?.let {
        result += GrailsPsiDirectoryNode(it, settings, data.first, data.second, data.third, ::shouldShowItem)
      }
    }

    PsiManager.getInstance(project).findDirectory(application.appRoot)?.let {
      result += OtherGrailsAppSourcesNode(it, settings)
    }

    return result
  }
}