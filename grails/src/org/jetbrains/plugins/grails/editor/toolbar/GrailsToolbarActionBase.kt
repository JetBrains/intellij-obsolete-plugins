/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.util.NlsActions.ActionText
import org.jetbrains.plugins.grails.actions.ArtefactData
import org.jetbrains.plugins.grails.actions.getArtefactData

abstract class GrailsToolbarActionBase : ActionGroup() {

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun displayTextInToolbar(): Boolean = true

  override fun update(e: AnActionEvent) {
    val data = getArtefactData(e.dataContext)
    e.presentation.text = data?.let { getTitle(it) } ?: templateText
    e.presentation.isPerformGroup = isOpenSingle() && data != null && createNavigateActions(data).size == 1
    e.presentation.isPopupGroup = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    assert(isOpenSingle())
    val data = getArtefactData(e.dataContext) ?: return
    createNavigateActions(data).singleOrNull()?.actionPerformed(e)
  }

  override fun getChildren(e: AnActionEvent?): Array<AnAction> = getArtefactData(e?.dataContext)?.let {
    createNavigateActions(it) + Separator() + createGenerateActions(it)
  }?.toTypedArray() ?: emptyArray()


  open fun isOpenSingle(): Boolean = true

  @ActionText abstract fun getTitle(artefactData: ArtefactData): String?

  abstract fun createNavigateActions(artefactData: ArtefactData): Collection<AnAction>

  open fun createGenerateActions(artefactData: ArtefactData): Collection<AnAction> = emptyList()
}
