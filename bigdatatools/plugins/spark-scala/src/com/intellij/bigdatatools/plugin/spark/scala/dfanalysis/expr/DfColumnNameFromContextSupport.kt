package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.AliasedColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeKeysUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition

class DfColumnNameFromContextSupport(override val folder: DfRecursiveExprFolder) : DfControlFlowTypeSupport<Pair<String?, DfColumnType?>>() {
  override fun asEndpoint(expr: ScExpression, context: DfExprFoldingContext): Pair<String?, DfColumnType?>? {
    if (DfComputingUtil.toTypeName(expr) != COLUMN_FQN || expr !is ScMethodCall) return null

    val funDef = DfComputingUtil.callToMethod(expr) ?: return null
    val callName = funDef.name() ?: return null
    if (!ENDPOINTS.contains(callName)) return null
    if (!DfComputingUtil.checkFunDefFqn(funDef)) return null

    val colName = DfComputingUtil.getParameterExpr(expr, COLUMN_NAME_PARAM)?.let { folder.foldString(it) }
    val tpeExpr = context.findExpr("cast", "to")
    val tpeString = tpeExpr?.let { DfComputingUtil.toTypeName(it) }

    val actualType =
      (if (callName == APPLY_NAME) processApply(colName, expr) else null)
      ?: processLitCall(expr)
      ?: if (tpeExpr == null || tpeString == null) UnknownType
      else {
        if (tpeString.endsWith(".String")) {
          folder.foldString(tpeExpr)?.let { ParseUtil.mapType(it) }
        }
        else {
          folder.foldColumnType(expr)
        }?.let { DfTypeKeysUtil.attach(it, DfTypeKeysUtil.CAST_KEY, true) } ?: UnknownType
      }

    val aliases = context.findExpr("alias", "alias")?.let { folder.foldString(it) }?.let { setOf(it) }
    val finalTpe =
      DfTypeKeysUtil.attachRange(
        if (aliases == null) actualType else AliasedColumnType.create(actualType, aliases),
        (expr as PsiElement).textRange
      )

    return Pair(colName, finalTpe)
  }

  override fun asEndpointFromString(literal: String): Pair<String?, DfColumnType?>? = null

  override fun transitMethodCall(call: ScMethodCall): List<ScExpression>? {
    if (DfComputingUtil.toTypeName(call) != COLUMN_FQN) return null

    return ((call.deepestInvokedExpr() as PsiElement).firstChild as? ScExpression)?.let { listOf(it) }
  }

  override fun transitConstructorCall(call: ScNewTemplateDefinition): List<ScExpression> = emptyList()

  override fun unionTypes(t1: Pair<String?, DfColumnType?>?, t2: Pair<String?, DfColumnType?>?): Pair<String?, DfColumnType?>? =
    if (t1 == null && t2 == null) null else Pair(t1?.first ?: t2?.first, t1?.second ?: t2?.second)

  override fun getCacheKey(): Key<DfComputingUtil.CachedValue<Pair<String?, DfColumnType?>>> = key

  private fun processApply(columnName: String?, call: ScMethodCall): DfColumnType? {
    if (columnName == null) return null

    return DfComputingUtil.findContext(call.deepestInvokedExpr())?.getColumnType(columnName)
  }

  companion object {
    private val key = Key.create<DfComputingUtil.CachedValue<Pair<String?, DfColumnType?>>>("DfCompoundColumTypeSupportCache")

    const val COLUMN_FQN = "org.apache.spark.sql.Column"
    private const val COLUMN_NAME_PARAM = "colName"

    private const val LIT_NAME = "lit"
    private const val COL_NAME = "col"
    private const val APPLY_NAME = "apply"

    private val ENDPOINTS = listOf(LIT_NAME, COL_NAME, APPLY_NAME)
  }
}