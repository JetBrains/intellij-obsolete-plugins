/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.NlsActions.ActionText
import org.jetbrains.plugins.grails.actions.ArtefactData
import javax.swing.Icon

/**
 * This class constructs navigate actions from targets.
 * It does not care about type of navigatable because actual navigate logic is delegated to inheritors.
 *
 * @param T type of target, e.g. PsiClass or VirtualFile.
 */
abstract class GrailsToolbarTargetAction<T> : GrailsToolbarActionBase() {

  override fun createNavigateActions(artefactData: ArtefactData): Collection<AnAction> = tryGetNavigateTargets(artefactData).map {
    object : AnAction(getNavigateTitle(it), null, getNavigateIcon(it)) {
      override fun actionPerformed(e: AnActionEvent) = navigate(artefactData, it)
    }
  }

  abstract fun getNavigateTargets(artefactData: ArtefactData): Collection<T>

  @ActionText abstract fun getNavigateTitle(target: T): String?

  abstract fun getNavigateIcon(target: T): Icon?

  abstract fun navigate(artefactData: ArtefactData, target: T): Unit

  private fun tryGetNavigateTargets(artefactData: ArtefactData): Collection<T> =
      if (DumbService.isDumb(artefactData.project)) emptyList()
      else getNavigateTargets(artefactData)
}