/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import javax.swing.Icon

open class GrailsPsiDirectoryNode(
  directory: PsiDirectory,
  settings: ViewSettings,
  val nodeIcon: Icon? = null,
  val nodeWeight: Int = 3,
  private val nodeTitle: String? = null,
  filter: ((PsiFileSystemItem) -> Boolean)? = null
) : PsiDirectoryNode(directory.project, directory, settings, filter) {

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    data.apply {
      nodeIcon?.let { setIcon(it) }
      nodeTitle?.let { presentableText = it }
    }
  }
}