/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.projectView.api.EP_NAME
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

class GrailsApplicationNode(
  application: GrailsApplication,
  viewSettings: ViewSettings
) : ProjectViewNode<GrailsApplication>(application.project, application, viewSettings) {

  override fun shouldUpdateData(): Boolean {
    return value.isValid && super.shouldUpdateData()
  }

  override fun getChildren(): Collection<AbstractTreeNode<*>> = EP_NAME.extensions.flatMap {
    it.createNodes(value, settings)
  }

  override fun contains(file: VirtualFile): Boolean {
    return project?.let { GrailsApplicationManager.getInstance(it).findApplication(file) == value } ?: false
  }

  override fun update(presentation: PresentationData) {
    presentation.apply {
      val application = value
      setIcon(application.icon)
      addText(application.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
      application.appVersion?.let { version ->
        addText(" ${version}", SimpleTextAttributes.REGULAR_ATTRIBUTES) // NON-NLS
      }
      addText(" " + GrailsBundle.message("project.view.application.node.version.label", application.grailsVersion), SimpleTextAttributes.REGULAR_ATTRIBUTES)
      tooltip = application.root.path
    }
  }
}
