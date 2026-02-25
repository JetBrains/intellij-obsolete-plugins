/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiClass
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.artefact.impl.controllers.getActions
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition

class GrailsControllerNode(clazz: PsiClass, settings: ViewSettings) : ClassTreeNode(clazz.project, clazz, settings) {

  val grailsApplication: GrailsApplication by lazyUnsafe { findNotNullValueOfType<GrailsApplication>() }

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>>? {
    if (!settings.isShowMembers) return null
    val clazz = value as? GrTypeDefinition ?: return null
    return getActions(clazz, grailsApplication).map {
      GrailsActionNode(it.key, it.value, settings)
    }
  }
}