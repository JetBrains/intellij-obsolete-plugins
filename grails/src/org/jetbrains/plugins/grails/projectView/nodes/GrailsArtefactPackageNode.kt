/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler

abstract class GrailsArtefactPackageNode<T : Any>(
  project: Project,
  settings: ViewSettings,
  value: T
) : GrailsArtefactHandlerNodeBase<T>(project, settings, value) {
  final override val artefactHandler: GrailsDisplayableArtefactHandler
    get() = findNotNullValueOfType()

  final override fun getTypeSortWeight(sortByType: Boolean): Int = 3

  abstract val packageFqn: String

  final override fun canRepresent(element: Any?): Boolean = canRepresentInner(element) || super.canRepresent(element)

  private fun canRepresentInner(element: Any?): Boolean = element is VirtualFile && element.isDirectory && element in nodeDirectories

  protected abstract val nodeDirectories: Collection<VirtualFile>

  protected fun packageDirectories(fqn: String): Collection<VirtualFile> {
    val package_ = JavaPsiFacade.getInstance(project!!).findPackage(fqn) ?: return emptyList()
    return package_.getDirectories(scope).map(PsiDirectory::getVirtualFile)
  }
}
