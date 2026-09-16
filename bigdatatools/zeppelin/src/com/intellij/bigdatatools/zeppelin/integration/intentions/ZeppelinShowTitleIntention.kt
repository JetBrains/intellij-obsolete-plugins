package com.intellij.bigdatatools.zeppelin.integration.intentions

import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinToggleCellTitleAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ZeppelinShowTitleIntention : ZeppelinIntentionBase(ZepMessagesBundle.message("intention.cell.title.show")), Iconable {
  override fun getIcon(flags: Int): Icon = AllIcons.Actions.RealIntentionBulb
  override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) = performCurCellAction(editor, ZeppelinToggleCellTitleAction.ID)
  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = getCurrentCell(editor)?.titleVisible == false
  override fun getFamilyName(): String = text
}