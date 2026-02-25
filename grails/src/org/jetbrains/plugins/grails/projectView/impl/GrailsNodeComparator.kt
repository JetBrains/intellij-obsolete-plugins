/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.impl.GroupByTypeComparator
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.plugins.grails.projectView.GrailsPluginsNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsApplicationNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactHandlerNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.projectView.nodes.OldGrailsPluginsNode

internal class GrailsNodeComparator(project: Project, id: String) : Comparator<NodeDescriptor<*>> {
  private val delegate = GroupByTypeComparator(project, id)

  override fun compare(left: NodeDescriptor<*>, right: NodeDescriptor<*>): Int {
    if (right is OldGrailsPluginsNode || right is GrailsPluginsNode) return -1
    if (left is OldGrailsPluginsNode || left is GrailsPluginsNode) return 1

    if (left is GrailsApplicationNode && right is GrailsApplicationNode) {
      return StringUtil.naturalCompare(left.value?.name, right.value?.name)
    }

    if (left is GrailsArtefactHandlerNode && right is GrailsArtefactHandlerNode) {
      return left.value.weight - right.value.weight
    }
    if (left is GrailsArtefactHandlerNode) return -1
    if (right is GrailsArtefactHandlerNode) return 1

    if (left is GrailsPsiDirectoryNode && right is GrailsPsiDirectoryNode) {
      return left.nodeWeight - right.nodeWeight
    }

    if (right is PsiFileNode && left !is PsiFileNode && right.parent is GrailsApplicationNode) {
      return -1
    }
    if (left is PsiFileNode && right !is PsiFileNode && left.parent is GrailsApplicationNode) {
      return 1
    }

    return delegate.compare(left, right)
  }
}