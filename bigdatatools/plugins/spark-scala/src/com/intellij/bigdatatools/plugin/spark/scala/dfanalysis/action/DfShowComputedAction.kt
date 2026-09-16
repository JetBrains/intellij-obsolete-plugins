package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.action

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfCompletionUtils
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.LightweightHint
import org.jetbrains.plugins.scala.actions.ScalaActionUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JComponent
import javax.swing.JLabel

class DfShowComputedAction : AnAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    if (!DfCompletionUtils.isEnabled()) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    ScalaActionUtil.enableAndShowIfInScalaFile(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val context = e.dataContext
    val project = e.project ?: return
    val editor = CommonDataKeys.EDITOR.getData(context) ?: return
    val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return
    val selectionModel = editor.getSelectionModel()

    val computed = if (selectionModel.hasSelection()) {
      (ScalaRefactoringUtil.getSelectedExpression(file, project, editor) as? PsiElement)?.let {
        DfComputingUtil.findTypeSchema(it)
      }
    }
    else {
      val offset = editor.logicalPositionToOffset(editor.getCaretModel().logicalPosition)
      val ref = file.findReferenceAt(offset) ?: file.findElementAt(offset)?.context ?: return
      var schema = (ref as? PsiElement)?.let {
        DfComputingUtil.findTypeSchema(it)
      }

      // Hack to retrieve schema
      if ((schema == null || schema.map.isEmpty()) && ref is ScMethodCall) {
        val left = (ref.deepestInvokedExpr() as PsiElement).firstChild
        schema = DfComputingUtil.findTypeSchema(left)
      }
      schema
    }

    computed ?: return

    showHint(editor, computed)
  }

  companion object {

    fun showHint(editor: Editor, schema: DfTypeSchema) {

      if (schema.map.isEmpty()) {
        return showHint(editor, JLabel(SparkScalaMessagesBundle.message("df.show.column.type.empty.df.message")))
      }

      val label = JLabel(ParseUtil.schemaToHtml(schema))

      showHint(editor, label)
    }

    private fun showHint(editor: Editor, component: JComponent) {
      val hint = LightweightHint(component)

      val hintManager = HintManagerImpl.getInstanceImpl()

      component.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
          hintManager.hideAllHints()
        }
      })

      val caretPosition = editor.getCaretModel().logicalPosition
      val hintPosition = HintManagerImpl.getHintPosition(hint, editor, caretPosition, HintManager.ABOVE)

      hintManager.showEditorHint(hint,
                                 editor,
                                 hintPosition,
                                 HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_SCROLLING,
                                 0,
                                 false)
    }
  }
}