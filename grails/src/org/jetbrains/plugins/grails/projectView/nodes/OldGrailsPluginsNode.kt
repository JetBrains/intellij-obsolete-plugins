/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.config.GrailsFramework
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.OldGrailsApplication

class OldGrailsPluginsNode(project: Project, settings: ViewSettings) : ProjectViewNode<String>(project, "plugins", settings) {

  val grailsApplication: OldGrailsApplication get() = parentValue as OldGrailsApplication

  override fun getChildren(): Collection<AbstractTreeNode<*>> {
    val manager = PsiManager.getInstance(grailsApplication.project)
    return GrailsFramework.getInstance().getCommonPluginRoots(grailsApplication.module, false).mapNotNull {
      manager.findDirectory(it)?.let {
        GrailsPsiDirectoryNode(it, settings, GroovyMvcIcons.Groovy_mvc_plugin, NodeWeights.FOLDER, it.name)
      }
    }
  }

  override fun update(data: PresentationData) {
    data.apply {
      setIcon(GroovyMvcIcons.Groovy_mvc_plugin)
      presentableText = "Plugins"
    }
  }

  override fun contains(file: VirtualFile): Boolean = children.any { it is GrailsPsiDirectoryNode && it.contains(file) }
}