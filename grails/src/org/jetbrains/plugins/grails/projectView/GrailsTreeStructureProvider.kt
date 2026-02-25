/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataSink
import org.jetbrains.plugins.grails.actions.GRAILS_APPLICATION
import org.jetbrains.plugins.grails.actions.GRAILS_ARTEFACT_HANDLER
import org.jetbrains.plugins.grails.actions.GRAILS_ARTEFACT_PACKAGE
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler
import org.jetbrains.plugins.grails.projectView.nodes.GrailsApplicationNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactHandlerNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactPackageNode
import org.jetbrains.plugins.grails.projectView.nodes.findValueOfType
import org.jetbrains.plugins.grails.structure.GrailsApplication

internal class GrailsTreeStructureProvider : TreeStructureProvider {
  override fun modify(parent: AbstractTreeNode<*>,
                      children: Collection<AbstractTreeNode<*>>,
                      settings: ViewSettings): Collection<AbstractTreeNode<*>> = children

  override fun uiDataSnapshot(sink: DataSink, selection: Collection<AbstractTreeNode<*>?>) {
    val single = selection.singleOrNull() ?: return
    sink.lazy(CommonDataKeys.VIRTUAL_FILE) {
      when (single) {
        is GrailsApplicationNode -> single.value.root
        is GrailsArtefactHandlerNode -> single.value.getDirectory(single.grailsApplication)
        else -> null
      }
    }
    sink.lazy(GRAILS_APPLICATION) {
      single.findValueOfType<GrailsApplication>()
    }
    sink.lazy(GRAILS_ARTEFACT_HANDLER) {
      single.findValueOfType<GrailsArtefactHandler>()
    }
    sink.lazy(GRAILS_ARTEFACT_PACKAGE) {
      (single as? GrailsArtefactPackageNode)?.packageFqn
    }
  }
}