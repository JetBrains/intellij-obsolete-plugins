package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition

// todo FQN checks !
class DfDdlStructTypeSupport(override val folder: DfRecursiveExprFolder) : DfControlFlowTypeSupport<MutableList<Pair<String, DfColumnType>>>() {
  override fun asEndpoint(expr: ScExpression, context: DfExprFoldingContext): MutableList<Pair<String, DfColumnType>>? {
    if ((expr !is ScNewTemplateDefinition || expr.name != STRUCT_FIELD_TYPE_NAME)
        && (expr !is ScMethodCall || (DfComputingUtil.callToMethod(expr)?.name() ?: DfComputingUtil.callToName(expr)) != "apply")) return null // cannot just look at identifier as there is no such identifier

    val paramExprs = DfComputingUtil.groupByParameters(expr, setOf(NAME_PARAM, DATA_TYPE_PARAM))
    val name = folder.foldString(paramExprs[NAME_PARAM]?.firstOrNull() ?: return null) ?: return null
    val dataType = folder.foldColumnType(paramExprs[DATA_TYPE_PARAM]?.firstOrNull() ?: return null) ?: return null

    return mutableListOf(Pair(name, dataType))
  }

  override fun asEndpointFromString(literal: String): MutableList<Pair<String, DfColumnType>>? {
    return null // todo probably we need to move ddl-parsing stuff here (?)
  }

  override fun transitMethodCall(call: ScMethodCall): List<ScExpression>? {
    if ((DfComputingUtil.callToMethod(call)?.name() ?: DfComputingUtil.callToName(call)) != "apply") return null
    val paramExprs = DfComputingUtil.groupByParameters(call, setOf(FIELDS_PARAM, ARRAY_XS))

    return paramExprs[FIELDS_PARAM] ?: paramExprs[ARRAY_XS]
  }

  override fun transitConstructorCall(call: ScNewTemplateDefinition): List<ScExpression>? {
    call.name?.equals(STRUCT_CLASS_TYPE_NAME) ?: return null
    return DfComputingUtil.groupByParameters(call, setOf(FIELDS_PARAM))[FIELDS_PARAM]
  }

  override fun unionTypes(t1: MutableList<Pair<String, DfColumnType>>?,
                          t2: MutableList<Pair<String, DfColumnType>>?): MutableList<Pair<String, DfColumnType>>? =
    if (t1 == null && t2 == null) null else {
      val result = mutableListOf<Pair<String, DfColumnType>>()
      t1?.let { result.addAll(it) }
      t2?.let { result.addAll(it) }
      result
    }

  override fun getCacheKey(): Key<DfComputingUtil.CachedValue<MutableList<Pair<String, DfColumnType>>>> = key

  companion object {
    private val key = Key.create<DfComputingUtil.CachedValue<MutableList<Pair<String, DfColumnType>>>>("DataFrameDdlTypeFields")

    private const val STRUCT_FIELD_TYPE_NAME = "StructField"
    private const val STRUCT_CLASS_TYPE_NAME = "StructType"
    private const val NAME_PARAM = "name"
    private const val DATA_TYPE_PARAM = "dataType"

    private const val FIELDS_PARAM = "fields"
    private const val ARRAY_XS = "xs"
  }
}