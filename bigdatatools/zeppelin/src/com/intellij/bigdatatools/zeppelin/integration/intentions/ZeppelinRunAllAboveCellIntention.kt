package com.intellij.bigdatatools.zeppelin.integration.intentions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunAllAboveAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ZeppelinRunAllAboveCellIntention : ZeppelinIntentionBase(NoteMessagesBundle.message("notebook.action.runAllAbove.intention")),
                                         Iconable {
  override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.NORMAL
  override fun getIcon(flags: Int): Icon = ZeppelinIcons.RUN_ALL
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) = performCurCellAction(editor, ZeppelinRunAllAboveAction.ID)
  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = isCurrentCellNotFirst(editor)
  override fun getFamilyName(): String = text
}