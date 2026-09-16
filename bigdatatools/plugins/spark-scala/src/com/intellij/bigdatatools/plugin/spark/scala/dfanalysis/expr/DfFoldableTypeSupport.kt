package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition

interface DfFoldableTypeSupport<T> {
  fun asEndpoint(expr: ScExpression, context: DfExprFoldingContext): T?

  fun asEndpointFromString(literal: String): T?

  fun transitMethodCall(call: ScMethodCall): List<ScExpression>?

  fun transitConstructorCall(call: ScNewTemplateDefinition): List<ScExpression>?

  fun unionTypes(t1: T?, t2: T?): T?

  fun getCacheKey(): Key<DfComputingUtil.CachedValue<T>>

  companion object {
    data class FromCallToParamTransit(val callName: String, val paramName: String)
  }
}

