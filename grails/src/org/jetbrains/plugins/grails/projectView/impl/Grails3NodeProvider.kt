/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.plugins.grails.projectView.GrailsPluginsNode
import org.jetbrains.plugins.grails.projectView.NodeWeights.SRC_FOLDERS
import org.jetbrains.plugins.grails.projectView.api.GrailsViewNodeProvider
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.util.version.Version.GRAILS_3_0

class Grails3NodeProvider : GrailsViewNodeProvider {

  private val specialFiles = listOf("build.gradle", "settings.gradle", "gradle.properties")
  private val specialDirs = listOf("src/main/scripts", "src/main/webapp")

  override fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    if (application.grailsVersion >= GRAILS_3_0) {
      val result = mutableListOf<AbstractTreeNode<*>>()

      val specialDirs = specialDirs.mapNotNull { application.findPsiDirectory(it) }

      result += specialDirs.map {
        GrailsPsiDirectoryNode(it, settings, nodeWeight = SRC_FOLDERS)
      }

      application.findPsiDirectory("src")?.let {
        val filter: (PsiFileSystemItem) -> Boolean = { it !in specialDirs && shouldShowItem(it) }
        result += GrailsPsiDirectoryNode(it, settings, nodeWeight = SRC_FOLDERS, filter = filter)
      }

      result += specialFiles.mapNotNull { application.findPsiFile(it) }.map {
        PsiFileNode(application.project, it, settings)
      }

      result += GrailsPluginsNode(application.project, settings)
      return result
    }
    return emptyList()
  }
}