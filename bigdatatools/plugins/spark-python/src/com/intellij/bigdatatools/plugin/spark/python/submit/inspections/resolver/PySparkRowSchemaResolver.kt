package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.types.TypeEvalContext

class PySparkRowSchemaResolver(context: TypeEvalContext) : PySparkPsiResolver<PySparkSchemaInfo>(context) {
  override fun beforeProcess(node: PsiElement): Pair<Boolean, PySparkSchemaInfo?> {
    if (node.typeFqnName != PySparkConsts.ROW_CLASS_NAME)
      return true to null
    return false to null
  }

  override fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, PySparkSchemaInfo?> {
    if (node !is PyCallExpression)
      return false to null
    val argumentNames = node.argumentList?.arguments?.mapNotNull {
      (it as? PyKeywordArgument)?.keyword
    } ?: emptyList()
    return true to PySparkSchemaInfo.create(isPartial = false, argumentNames)
  }
}