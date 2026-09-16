package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.TypeEvalContext

@Suppress("unused")
object PySparkUtils {
  internal fun PyArgumentList.getArgByNameOrIndex(name: String, index: Int): PyExpression? {
    return getKeywordArgument(name)?.valueExpression ?: arguments.getOrNull(index)
  }

  internal fun PyCallExpression.getArgByNameOrIndex(name: String, index: Int): PyExpression? {
    return argumentList?.getArgByNameOrIndex(name, index)
  }

  fun resolveReference(node: PsiElement, context: TypeEvalContext): PsiElement {
    when (node) {
      is PyReferenceExpression -> {
        return node.followAssignmentsChain(PyResolveContext.defaultContext(context)).element ?: node
      }
      is PyAssignmentStatement -> {
        val curNode = node.assignedValue ?: return node
        return resolveReference(curNode, context)
      }
      else -> return PyUtil.resolveToTheTop(node)
    }
  }


  fun resolveInvokeParentDf(psiElement: PsiElement, context: TypeEvalContext): PySparkSchemaInfo? {
    if (psiElement is PyCallExpression) {
      val referenceExpression = psiElement.callee as? PyReferenceExpression
      val schema = referenceExpression?.qualifier?.let { PySparkDataFrameResolver.resolveDfSchema(it, context) }
      if (schema != null)
        return schema
    }
    val parent = psiElement.parent ?: return null
    return resolveInvokeParentDf(parent, context)
  }

  fun PsiElement.isParentOf(element: PsiElement): Boolean {
    if (element == this)
      return false

    val parent = element.parent ?: return false
    if (parent is PsiFile)
      return false

    return isParentOf(parent)
  }
}