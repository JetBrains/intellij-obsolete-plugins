package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.types.TypeEvalContext

class SchemaByDataResolver(context: TypeEvalContext) : PySparkPsiResolver<PySparkSchemaInfo>(context) {
  override fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, PySparkSchemaInfo?> {
    if (node !is PySequenceExpression)
      return false to null
    val firstRow = node.elements.firstOrNull() ?: return true to null
    if (firstRow.typeFqnName != PySparkConsts.ROW_CLASS_NAME)
      return true to null

    val schema = PySparkRowSchemaResolver(context).resolve(firstRow)
    return true to schema
  }
}