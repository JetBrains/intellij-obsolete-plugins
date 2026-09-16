package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.TypeEvalContext

open class AbstractPySparkResolver(val context: TypeEvalContext) {
  private val resolveContext = PyResolveContext.defaultContext(context)

  protected val PsiElement.typeFqnName: String?
    get() {
      val pyTypedElement = this as? PyTypedElement ?: return null
      val pyClassType = this@AbstractPySparkResolver.context.getType(pyTypedElement) as? PyClassType

      return pyClassType?.classQName
    }

  protected fun getInvokeNode(curNode: PsiElement): PsiElement? {
    return when (curNode) {
      is PyCallExpression -> {
        val callee = curNode.callee as? PyReferenceExpression ?: return null
        getInvokeNode(callee)
      }
      is PyReferenceExpression -> {
        curNode.qualifier ?: curNode.followAssignmentsChain(resolveContext).element
      }
      is PyTargetExpression -> {
        curNode.findAssignedValue()
      }
      is PyAssignmentStatement -> {
        curNode.assignedValue
      }
      is PyParenthesizedExpression -> {
        curNode.containedExpression
      }
      else -> null
    }
  }

  protected fun resolveReferences(node: PsiElement) = PySparkUtils.resolveReference(node, context)
}