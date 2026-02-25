/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.plugins.GrailsPluginDescriptor
import org.jetbrains.plugins.grails.structure.GrailsApplication

class GrailsPluginNode(
    project: Project,
    value: GrailsPluginDescriptor,
    settings: ViewSettings
) : AbstractPsiBasedNode<GrailsPluginDescriptor>(project, value, settings) {

  init {
    myName = value.pluginName
  }

  val grailsApplication: GrailsApplication by lazyUnsafe { findNotNullValueOfType<GrailsApplication>() }

  override fun extractPsiFromValue(): PsiClass? = value?.pluginClass

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> = emptyList()

  override fun isAlwaysLeaf(): Boolean = true

  override fun updateImpl(data: PresentationData) {
    data.apply {
      setIcon(GroovyMvcIcons.Groovy_mvc_plugin)
      addText(value.pluginName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
      addText(" ${value.pluginVersion ?: grailsApplication.grailsVersion}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
  }
}