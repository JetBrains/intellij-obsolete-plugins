/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.actions.ArtefactData
import javax.swing.Icon

abstract class GrailsToolbarVfileAction : GrailsToolbarTargetAction<VirtualFile>() {

  @ActionText override fun getNavigateTitle(target: VirtualFile): String = target.name

  override fun getNavigateIcon(target: VirtualFile): Icon? = VirtualFilePresentation.getIcon(target)

  override fun navigate(artefactData: ArtefactData, target: VirtualFile): Unit = PsiNavigationSupport.getInstance().createNavigatable(artefactData.project, target, -1).navigate(true)
}
