package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.ConstantExpressionEvaluator
import com.intellij.util.containers.tail
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScIf
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScParenthesisedExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.util.ScalaConstantExpressionEvaluator

class DfRecursiveExprFolder {
  fun foldString(expr: ScExpression): String? {
    val folder = ScalaConstantExpressionEvaluator() as ConstantExpressionEvaluator
    return folder.computeConstantExpression(expr as PsiElement, false)?.toString()
  }

  fun foldColumnType(expr: ScExpression): DfColumnType? =
    foldTypeInner(expr, DfColumnTypeExprSupport(this))

  fun foldDdlStructType(expr: ScExpression): List<Pair<String, DfColumnType>>? {
    return foldTypeInner(expr, DfDdlStructTypeSupport(this))
  }

  fun foldContextType(expr: ScExpression): Pair<String?, DfColumnType?>? {
    return foldTypeInner(expr, DfColumnNameFromContextSupport(this))
  }

  private fun <T> foldTypeInner(expr: ScExpression,
                                typeSupport: DfFoldableTypeSupport<T>,
                                context: DfExprFoldingContext = DfExprFoldingContext(DfComputingUtil.DF_COLUMN_TYPE_COMPUTING_DEPTH)): T? {
    if (!context.treeDown()) return null

    val alreadyComputed = DfComputingUtil.getFromElement(
      typeSupport.getCacheKey(),
      expr as PsiElement
    )


    if (alreadyComputed != null) return alreadyComputed

    typeSupport.asEndpoint(expr, context)?.let { return it }

    context.addToContext(expr)

    fun unionAll(paramExprs: List<ScExpression>): T? =
      if (paramExprs.isEmpty()) null
      else {
        val computed = paramExprs.map { foldTypeInner(it, typeSupport, context) }
        computed.tail().fold(computed.firstOrNull()) { acc: T?, t: T? -> typeSupport.unionTypes(acc, t) }
      }

    when (expr) {
      is ScLiteral -> {
        return foldString(expr)?.let { typeSupport.asEndpointFromString(it) }
      }
      is ScBlockExpr -> {
        val maybeResult = expr.resultExpression()
        return if (maybeResult.isDefined) foldTypeInner(maybeResult.get(), typeSupport, context) else null
      }
      is ScIf -> {
        val maybeThen = expr.thenExpression()
        val maybeElse = expr.elseExpression()

        return if (maybeThen.isDefined && maybeElse.isDefined) {
          val t = foldTypeInner(maybeThen.get(), typeSupport, context)
          val e = foldTypeInner(maybeElse.get(), typeSupport, context)

          typeSupport.unionTypes(t, e)
        }
        else null
      }
      is ScMethodCall -> {
        typeSupport.transitMethodCall(expr)?.let { paramExprs -> return unionAll(paramExprs) }
        return ((expr.deepestInvokedExpr() as? PsiReference)?.resolve() as? ScExpression)?.let { foldTypeInner(it, typeSupport, context) }
      }
      is ScFunctionDefinition -> {
        val body = expr.body()
        return if (body.isEmpty) null else foldTypeInner(body.get(), typeSupport, context)
      }
      is ScNewTemplateDefinition -> {
        typeSupport.transitConstructorCall(expr)?.let { paramExprs -> return unionAll(paramExprs) }
        return null
      }
      is ScReferenceExpression -> {
        val maybeExpr = (expr as PsiReference).resolve() as? ScalaPsiElement ?: return null

        return when (maybeExpr) {
          is ScExpression -> foldTypeInner(maybeExpr, typeSupport, context)
          is ScPattern -> {
            val nameContext = (maybeExpr as? ScNamedElement)?.nameContext()
            if (nameContext is ScValueOrVariableDefinition) {
              val maybeBody = nameContext.expr()
              if (maybeBody.isEmpty) null else foldTypeInner(maybeBody.get(), typeSupport, context)
            }
            else null
          }
          else -> null
        }

        //val resolved = (expr as PsiReference).resolve() as? ScExpression ?: return null
        //return foldTypeInner(resolved, typeSupport, context)
      }
      is ScAssignment -> {
        val right = expr.rightExpression()
        if (right.isEmpty) return null
        return foldTypeInner(right.get(), typeSupport, context)
      }
      is ScParenthesisedExpr -> {
        val inner = expr.innerElement()
        if (inner.isEmpty) return null

        return foldTypeInner(inner.get(), typeSupport, context)
      }
      else -> return null
    }
  }
}