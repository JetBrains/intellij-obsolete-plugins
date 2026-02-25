/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.api

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

interface GrailsDisplayableArtefactHandler : GrailsArtefactHandler, IconOwner {

  val groupIcon: Icon get() = icon

  val title: String

  val weight: Int get() = 0

  fun isVisible(application: GrailsApplication): Boolean = true

  fun createNode(artefact: PsiClass, settings: ViewSettings): AbstractTreeNode<*>? = null
}