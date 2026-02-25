/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

typealias TreeNodes = Collection<AbstractTreeNode<*>>
typealias Classes = Collection<PsiClass>

internal inline fun <reified T : Any> AbstractTreeNode<*>.findNotNullValueOfType(): T {
  return findValueOfType()!!
}

internal inline fun <reified T : Any> AbstractTreeNode<*>.findValueOfType(): T? {
  var current: AbstractTreeNode<*>? = this.parent
  while (current != null) {
    val value = current.value
    if (value is T) return value
    current = current.parent
  }
  return null
}

fun mayContain(application: GrailsApplication, file: VirtualFile): Boolean {
  val psiFile = PsiManager.getInstance(application.project).findFile(file) ?: return false
  return psiFile is GroovyFile && psiFile.classes.size == 1
}
