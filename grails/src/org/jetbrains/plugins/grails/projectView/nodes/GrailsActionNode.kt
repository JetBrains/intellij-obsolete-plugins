/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.BasePsiMemberNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiMember
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.util.GrailsUtils

class GrailsActionNode(
  val actionName: String,
  action: PsiMember,
  settings: ViewSettings
) : BasePsiMemberNode<PsiMember>(action.project, action, settings) {

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    return GrailsUtils.getViewPsiByAction(value).map { PsiFileNode(project, it, settings) }
  }

  override fun updateImpl(data: PresentationData) {
    data.apply {
      presentableText = actionName
      setIcon(GroovyMvcIcons.Action_method)
    }
  }
}