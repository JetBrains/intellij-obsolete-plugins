/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.plugins.computePlugins
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPluginNode
import org.jetbrains.plugins.grails.structure.GrailsApplication

class GrailsPluginsNode(project: Project, settings: ViewSettings) : ProjectViewNode<String>(project, "plugins", settings) {

  val grailsApplication: GrailsApplication get() = parentValue as GrailsApplication

  override fun getChildren(): Collection<AbstractTreeNode<*>> {
    val application = grailsApplication
    val project = application.project
    return application.computePlugins().map { GrailsPluginNode(project, it, settings) }
  }

  override fun contains(file: VirtualFile): Boolean = children.any { it is ProjectViewNode && it.contains(file) }

  override fun update(presentation: PresentationData) {
    presentation.apply {
      setIcon(GroovyMvcIcons.Groovy_mvc_plugin)
      presentableText = "Plugins"
    }
  }
}