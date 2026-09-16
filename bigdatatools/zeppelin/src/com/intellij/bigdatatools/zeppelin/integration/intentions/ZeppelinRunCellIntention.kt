package com.intellij.bigdatatools.zeppelin.integration.intentions

import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRunCellAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ZeppelinRunCellIntention : ZeppelinIntentionBase(ZepMessagesBundle.message("run.paragraph.desc")), Iconable {
  override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH
  override fun getIcon(flags: Int): Icon = AllIcons.Actions.Execute
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) = performCurCellAction(editor, ZeppelinRunCellAction.ID)
  override fun getFamilyName(): String = text
}