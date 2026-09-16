package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

class DfExprFoldingContext(maxDepth: Int) {
  private var depth = maxDepth
  private val contextExprs = hashMapOf<String, Map<String, List<ScExpression>>>()

  fun treeDown(): Boolean = depth-- > 0

  fun addToContext(expr: ScExpression) {
    val name = DfComputingUtil.callToName(expr) ?: return
    // since we go from root to leaves and in scala plugin earlier exprs are deeper,
    // if we encountered some param name => we have the up-to-date version of it
    if (contextExprs.containsKey(name)) return

    contextExprs[name] = DfComputingUtil.groupByParameters(expr, emptySet())
  }

  fun findAllExprs(callName: String, paramName: String): List<ScExpression>? = contextExprs[callName]?.get(paramName)

  fun findExpr(callName: String, paramName: String): ScExpression? = findAllExprs(callName, paramName)?.firstOrNull()
}