package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr.DfRecursiveExprFolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition

abstract class DfAbstractMethodBasedTypeProvider : DfTypeSourceProvider {

  companion object {
    @JvmStatic
    protected fun <T> foldParamWith(call: ScMethodCall, paramName: String, folding: (ScExpression) -> T?): T? {
      val pathParam = call.matchedParameters().find { it._2().name() == paramName }
      if (pathParam.isEmpty) return null

      return folding.invoke(pathParam.get()._1())
    }

    @JvmStatic
    internal fun foldStringParam(call: ScMethodCall, paramName: String): String? = foldParamWith(call, paramName) {
      DfRecursiveExprFolder().foldString(it)
    }
  }

  protected fun foldStringVararg(call: ScMethodCall, paramName: String): List<String> {
    val result = mutableListOf<String>()
    val folder = DfRecursiveExprFolder()
    for (f in DfComputingUtil.handleVarargParams(call.matchedParameters()).reverse().filter { it._2().name() == paramName })
      folder.foldString(f._1())?.let { result.add(it) }

    return result
  }

  protected fun checkMethodCallExpr(maybeExpr: PsiElement, acceptedNames: Set<String>, acceptedClassNames: Set<String>): Boolean {
    val parent = maybeExpr.parent
    if (maybeExpr !is ScExpression) return false
    if (parent !is ScMethodCall) return false

    val callExpr = parent.deepestInvokedExpr()
    if (!acceptedNames.contains((maybeExpr as PsiElement).lastChild.text)) return false

    val resolved = (callExpr as? PsiReference)?.resolve()

    return resolved is ScFunctionDefinition && acceptedClassNames.contains((resolved as PsiMember).containingClass?.name)
  }
}