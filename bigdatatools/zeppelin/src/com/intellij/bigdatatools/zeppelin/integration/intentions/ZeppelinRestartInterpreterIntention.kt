package com.intellij.bigdatatools.zeppelin.integration.intentions

import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinRestartInterpreterAction
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ZeppelinRestartInterpreterIntention : ZeppelinIntentionBase(ZepMessagesBundle.message("interpreter.restart.desc")), Iconable {
  override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
  override fun getIcon(flags: Int): Icon = AllIcons.Actions.Restart
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) = performCurCellAction(editor, ZeppelinRestartInterpreterAction.ID)
  override fun getFamilyName(): String = text
}