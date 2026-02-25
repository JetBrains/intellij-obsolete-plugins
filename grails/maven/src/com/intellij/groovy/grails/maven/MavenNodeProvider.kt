/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.intellij.groovy.grails.maven

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiManager
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.plugins.grails.projectView.api.GrailsSingleNodeProvider
import org.jetbrains.plugins.grails.structure.GrailsApplication

internal class MavenNodeProvider : GrailsSingleNodeProvider() {

  override fun createNode(application: GrailsApplication, settings: ViewSettings): AbstractTreeNode<*>? {
    if (application !is GrailsMavenApplication) return null
    val project = application.project
    return application.root.findChild(MavenConstants.POM_XML)
      ?.let { PsiManager.getInstance(project).findFile(it) }
      ?.let { PsiFileNode(project, it, settings) }
  }
}