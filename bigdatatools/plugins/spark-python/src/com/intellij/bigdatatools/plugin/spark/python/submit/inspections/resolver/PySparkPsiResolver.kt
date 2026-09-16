package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

open class PySparkPsiResolver<T>(context: TypeEvalContext) : AbstractPySparkResolver(context) {
  open fun beforeProcess(node: PsiElement): Pair<Boolean, T?> = false to null
  open fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, T?> = false to null
  open fun afterProcess(node: PsiElement, data: T?): T? = data

  fun resolve(node: PsiElement): T? {
    val (shouldCancel, result) = beforeProcess(node)
    if (shouldCancel)
      return result

    val invoker = getInvokeNode(node)
    val (isProcessed, processResult) = customProcess(node, invoker)
    val finalResult = if (isProcessed)
      return processResult
    else
      invoker?.let { resolve(it) }

    return afterProcess(node, finalResult)
  }

  companion object {
    fun <T : Any> resolveCustom(context: TypeEvalContext,
                                node: PsiElement,
                                processFun: (PsiElement, PsiElement?) -> Pair<Boolean, T?>): T? {
      val resolver = object : PySparkPsiResolver<T>(context) {
        override fun customProcess(node: PsiElement, invokeNode: PsiElement?) = processFun(node, invokeNode)
      }
      return resolver.resolve(node)
    }


    fun resolveCallExpressionWithType(context: TypeEvalContext, node: PsiElement, type: String?): PyCallExpression? {
      val resolver = object : PySparkPsiResolver<PyCallExpression>(context) {
        override fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, PyCallExpression?> {
          if (type != null && node.typeFqnName != type)
            return true to null

          if (node is PyCallExpression)
            return true to node
          else
            return false to null
        }
      }
      return resolver.resolve(node)
    }

    fun resolveString(context: TypeEvalContext, node: PsiElement): String? {
      val resolver = object : PySparkPsiResolver<String>(context) {
        override fun customProcess(node: PsiElement, invokeNode: PsiElement?): Pair<Boolean, String?> {
          return when (node) {
            is PyStringLiteralExpression -> true to node.stringValue
            else -> false to null
          }
        }
      }
      return resolver.resolve(node)
    }
  }
}