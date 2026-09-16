package com.intellij.bigdatatools.zeppelin.frontend

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableViewManager
import com.intellij.bigdatatools.zeppelin.ztools.variableview.ZeppelinDebugNode
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.platform.debugger.impl.ui.actions.CustomQuickEvaluateActionProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.xdebugger.impl.evaluate.quick.common.AbstractValueHint
import com.intellij.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler
import com.intellij.xdebugger.impl.evaluate.quick.common.ValueHintType
import java.awt.Point

internal class ZeppelinCustomQuickEvaluateActionProvider : CustomQuickEvaluateActionProvider {
  override fun getCustomQuickEvaluateHandler(project: Project): QuickEvaluateHandler? {
    return ZeppelinEvaluateHandler()
  }
}

private class ZeppelinEvaluateHandler : QuickEvaluateHandler() {
  override fun isEnabled(project: Project) = true

  override fun createValueHint(project: Project, editor: Editor, point: Point, type: ValueHintType?): AbstractValueHint? {
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
    val notebookVirtualFile = psiFile.virtualFile as? NotebookVirtualFile ?: return null
    val root = notebookVirtualFile.getUserData(VariableViewManager.Companion.VARIABLE_VIEW_KEY)?.root ?: return null

    val element = psiFile.findElementAt(editor.caretModel.offset) ?: return null
    val range = TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd + 1)

    return ZeppelinTypeHint(root, element.text, project, editor, point, range)
  }

  override fun canShowHint(project: Project): Boolean = isEnabled(project)

  override fun getValueLookupDelay(project: Project?): Int = 700
}

private class ZeppelinTypeHint(val root: ZeppelinDebugNode,
                               @NlsSafe val targetName: String,
                               project: Project,
                               editor: Editor,
                               point: Point,
                               textRange: TextRange) : AbstractValueHint(
  project, editor, point, ValueHintType.MOUSE_CLICK_HINT, textRange
) {
  override fun evaluateAndShowHint() {
    val result = root.children?.find { it.name == targetName } ?: return
    val component = HintUtil.createInformationComponent()
    val textValue = result.computeTextValue()

    component.append(
      "$targetName = ${
        textValue.ifEmpty {
          result.children?.joinToString(separator = "; ", prefix = "[", postfix = "]") {
            "${it.name} = ${it.computeTextValue()}"
          } ?: ""
        }
      }")
    showHint(component)
  }
}