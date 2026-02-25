/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.javaee.JavaeeIcons
import com.intellij.lang.Language
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformIcons
import icons.JetgroovyIcons
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.projectView.NodeWeights.SRC_FOLDERS
import org.jetbrains.plugins.grails.projectView.NodeWeights.TESTS_FOLDER
import org.jetbrains.plugins.grails.projectView.api.GrailsViewNodeProvider
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.projectView.nodes.OldGrailsPluginsNode
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.OldGrailsApplication
import java.util.Locale

class OldGrailsNodeProvider : GrailsViewNodeProvider {

  override fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    if (application is OldGrailsApplication) {
      val result = mutableListOf<AbstractTreeNode<*>>()
      val project = application.project
      val manager = PsiManager.getInstance(project)

      application.findPsiFile("application.properties")?.let {
        result += PsiFileNode(project, it, settings)
      }

      application.findPsiDirectory("web-app")?.let {
        result += GrailsPsiDirectoryNode(
          it, settings, JavaeeIcons.WEB_FOLDER_CLOSED, NodeWeights.WEB_APP_FOLDER, "web-app"
        )
      }

      application.findPsiDirectory("scripts")?.let {
        result += GrailsPsiDirectoryNode(
            it, settings, JetgroovyIcons.Groovy.Gant_16x16, SRC_FOLDERS, "Scripts"
        )
      }

      application.root.children.find { it.name.endsWith("GrailsPlugin.groovy") }?.let {
        manager.findFile(it)?.let {
          result += PsiFileNode(project, it, settings)
        }
      }

      application.root.findChild("src")?.let { srcDir ->
        for (child in srcDir.children) {
          manager.findDirectory(child)?.let {
            val name = child.name
            val icon = (Language.findLanguageByID(name.capitalize()) ?: Language.findLanguageByID(name.uppercase(Locale.getDefault())))
                ?.associatedFileType?.icon ?: PlatformIcons.SOURCE_FOLDERS_ICON
            result += GrailsPsiDirectoryNode(it, settings, icon, SRC_FOLDERS, "Sources:$name", ::shouldShowItem)
          }
        }
      }

      application.root.findChild("test")?.let { testDir ->
        for (child in testDir.children) {
          manager.findDirectory(child)?.let {
            val name = child.name
            val icon = when (name) {
              "unit" -> PlatformIcons.TEST_SOURCE_FOLDER
              "functional" -> GroovyMvcIcons.Grails_test
              "integration" -> GroovyMvcIcons.Grails_test
              else -> PlatformIcons.TEST_SOURCE_FOLDER
            }
            result += GrailsPsiDirectoryNode(it, settings, icon, TESTS_FOLDER, "Tests:$name")
          }
        }
      }

      result += OldGrailsPluginsNode(project, settings)

      return result
    }

    return emptyList()
  }
}