package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.lang.jvm.JvmNamedElement
import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.xdebugger.ui.DebuggerColors
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement

class ZeppelinVariableLinePainter : EditorLinePainter() {
  companion object {
    private const val SCALA_LANG_ID = "Scala"
    private const val PYTHON_LANG_ID = "Python"

    private fun supportedLanguages(): List<String> = listOf(SCALA_LANG_ID, PYTHON_LANG_ID)
    private const val MAGIC_OFFSET = 6
  }

  fun isEnabled() = false

  override fun getLineExtensions(project: Project, file: VirtualFile, lineNumber: Int): List<LineExtensionInfo> {
    if (!isEnabled()) return emptyList()

    val zeppelinFile = file as? NotebookVirtualFile ?: return emptyList()
    val varView = zeppelinFile.getUserData(VariableViewManager.VARIABLE_VIEW_KEY) ?: return emptyList()

    if (!varView.isUpToDate()) return emptyList()
    varView.getCachedInfo(lineNumber)?.let { return createInfo(it) }

    val psiFile = PsiManager.getInstance(project).findFile(zeppelinFile) ?: return emptyList()
    val document = PsiDocumentManager.getInstance(project).getCachedDocument(psiFile) ?: return emptyList()
    val lineStartOffset = document.getLineStartOffset(lineNumber)

    val psiElement = psiFile.findElementAt(lineStartOffset) ?: return emptyList()
    val langId = psiElement.language.id
    if (langId !in supportedLanguages())
      return emptyList()

    val viewRoot = varView.root
    val interpreterNodes = when (langId) {
                            PYTHON_LANG_ID -> viewRoot.children?.filter { it.name.endsWith(".pyspark") }
                            SCALA_LANG_ID -> viewRoot.children?.filter { it.name.endsWith(".spark") }
                            else -> null
                          } ?: emptyList()

    interpreterNodes.ifEmpty { return emptyList() }

    val targetNames = getNameSequence(psiFile, psiElement, lineStartOffset, document.getLineEndOffset(lineNumber))
      .toSet().ifEmpty { return emptyList() }



    val debugValues = interpreterNodes.flatMap {
      it.children ?: emptyList()
    }.filter { it.name in targetNames }.ifEmpty { return emptyList() }
    val str = "    {${debugValues.joinToString(separator = "; ") { "${it.name} = ${it.computeTextValue()}" }}}"
    varView.cacheLineInfo(lineNumber, str)
    return createInfo(str)
  }

  private fun createInfo(@NlsSafe str: String): List<LineExtensionInfo> = listOf(
    LineExtensionInfo(str, EditorColorsManager.getInstance().globalScheme.getAttributes(DebuggerColors.INLINED_VALUES)
    ))

  private fun getNameSequence(file: PsiFile, psiElement: PsiElement, startOffsetGuard: Int, endOffsetGuard: Int): List<String?> {
    val result = mutableSetOf<String>()

    fun getNameInner(startElement: PsiElement, startOffsetGuard: Int): PsiElement? {
      var c: PsiElement? = startElement

      while (c != null && c.textRange.startOffset >= startOffsetGuard) {
        when (c) {
          is ScPatternDefinition -> {
            val iterator = c.bindings().iterator()

            while (iterator.hasNext()) result.add(iterator.next().name())
            return c
          }

          is ScNamedElement -> {
            result.add(c.name())
            return c
          }

          is JvmNamedElement -> {
            c.name?.let { result.add(it) }
            return c
          }

          is PsiNamedElement -> {
            c.name?.let { result.add(it) }
            return c
          }

          else -> c = c.parent
        }
      }

      return null
    }

    var last: PsiElement? = psiElement

    while (last != null && last.textRange.startOffset >= startOffsetGuard && last.textRange.endOffset <= endOffsetGuard - MAGIC_OFFSET) {
      val offset = getNameInner(last, startOffsetGuard)?.textRange?.endOffset
      last = if (offset == null) null else file.findElementAt(offset + 1)
    }

    return result.toList()
  }
}