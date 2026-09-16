package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala

import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScReferenceExpressionImpl

class ZeppelinScalaResUsageFilter : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (element.containingFile.viewProvider !is ZeppelinFileViewProvider) return

        when (element) {
          is ScReferenceExpressionImpl -> {
            val txt = element.text
            if (txt.length > 3 && txt.startsWith("res") && txt.drop(3).all { it.isDigit() } ) {
              holder.registerProblem(element, null, ZepMessagesBundle.message("zeppelin.res.usage.inspection"))
            }
          }
        }

        super.visitElement(element)
      }
    }
  }
}