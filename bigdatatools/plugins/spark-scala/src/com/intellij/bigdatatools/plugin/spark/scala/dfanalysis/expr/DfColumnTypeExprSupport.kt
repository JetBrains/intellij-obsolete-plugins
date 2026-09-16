package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.BooleanType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnTypesUtil
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DoubleType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.FloatType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.IntegerType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.LongType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ShortType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.StringType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfComputableTypes
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeKeysUtil
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.OuterComputableType1
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.OuterComputableType2
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.scala.DfScalaTypesMapper
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScPrefixExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition

class DfColumnTypeExprSupport(override val folder: DfRecursiveExprFolder) : DfControlFlowTypeSupport<DfColumnType>() {
  override fun asEndpoint(expr: ScExpression, context: DfExprFoldingContext): DfColumnType? =
    asEndpointImpl(expr)?.let { DfTypeKeysUtil.attachRange(it, (expr as PsiElement).textRange) }

  override fun asEndpointFromString(literal: String): DfColumnType? = DF_COLUMN_TYPE_LIST[literal]

  override fun transitMethodCall(call: ScMethodCall): List<ScExpression>? {  // todo write down .cast call
    if (!DfComputingUtil.checkMethodCallFqn(call)) return null
    return fromCallToParams(DfComputingUtil.callToName(call) ?: return null, call)
  }

  override fun transitConstructorCall(call: ScNewTemplateDefinition): List<ScExpression>? {
    val name = call.name ?: return null
    return fromCallToParams(name, call)
  }

  override fun unionTypes(t1: DfColumnType?, t2: DfColumnType?): DfColumnType? = t1?.union(t2) ?: t2?.union(t1)

  override fun getCacheKey(): Key<DfComputingUtil.CachedValue<DfColumnType>> = COMPUTED_COLUMN_TYPE

  private fun asEndpointImpl(expr: ScExpression): DfColumnType? {
    val isColumnType = DfComputingUtil.toTypeName(expr)?.startsWith(DfComputingUtil.APACHE_SPARK_PACKAGE) ?: false

    if (isColumnType) {
      processLitCall(expr)?.let { return it }
      processParameterlessCall(expr)?.let { return it }
      (expr as? ScPrefixExpr)?.let { processPrefixCall(it) }?.let { return it }
      (expr as? ScMethodCall)?.let { processSingleMethodCall(expr) }?.let { return it }
      (expr as? ScInfixExpr)?.let { processInfixCall(it) }?.let { return it }
      (expr as? ScMethodCall)?.let { processDfFunctionCall(it) }?.let { return it }
    }

    return DfComputingUtil.toTypeName(expr)?.let { PARTICULAR_TYPES_LIST[it] }
  }

  private fun fromCallToParams(name: String, call: ScExpression): List<ScExpression>? {
    val methodTransit = PARTICULAR_CALLS_LIST[name] ?: return null
    return DfComputingUtil.groupByParameters(call, setOf(methodTransit.paramName))[methodTransit.paramName]
  }

  //a.isNan
  private fun processParameterlessCall(expr: ScExpression): DfColumnType? =
    (expr as PsiElement).lastChild?.text?.let { idComputable[it] }

  //sum(a) . We don't handle cases like 'Foo.<apply>()' here
  private fun processSingleMethodCall(call: ScMethodCall): DfColumnType? {
    val name = DfComputingUtil.callToName(call) ?: return null
    idComputable[name]?.let { return it }

    val params = call.matchedParameters()
    if (params.length() != 1) return null
    val param = params.head()._1()

    return processUnary(name, param)
  }

  //!a
  private fun processPrefixCall(call: ScPrefixExpr): DfColumnType? {
    val arg = call.operand() ?: return null
    val name = call.operation()?.refName() ?: return null

    return processUnary(name, arg)
  }

  // a - b
  private fun processInfixCall(infixCall: ScInfixExpr): DfColumnType? {
    val left = infixCall.left() ?: return null
    val right = infixCall.right() ?: return null

    val name = ((infixCall.operation() as? PsiReference)?.resolve() as? ScFunctionDefinition)?.name ?: return null

    return processBinary(name, left, right, (infixCall.operation() as PsiElement).textRange)
  }

  // a.minus(b) . We don't handle cases like 'a.<apply>(b)' here
  private fun processDfFunctionCall(call: ScMethodCall): DfColumnType? {
    val name = DfComputingUtil.callToName(call) ?: return null
    idComputable[name]?.let { return it }

    val params = call.matchedParameters()
    if (params.isEmpty) return null

    val right = params.head()._1()
    val left = (call.deepestInvokedExpr() as PsiElement).firstChild as? ScReferenceExpression ?: return null

    return processBinary(name, left, right, (call.deepestInvokedExpr() as PsiElement).textRange)
  }

  private fun processUnary(name: String, arg: ScExpression): DfColumnType? {
    val computable = unaryComputable[name]?.createType((arg as PsiElement).textRange) ?: return null
    val pair = folder.foldContextType(arg)

    val argTpe = pair?.second ?: folder.foldColumnType(arg)
    val argName = pair?.first

    return if (argTpe != null && !DfComputingUtil.isEmpty(argTpe))
      computable.compute(argTpe)
    else if (argName != null)
      object : OuterComputableType1(argName, computable.baseType) {
        override fun computeImpl(t: DfColumnType): DfColumnType = computable.compute(t)
      }
    else
      computable.compute(null)
  }

  private fun processBinary(name: String, left: ScExpression, right: ScExpression, range: TextRange): DfColumnType? {
    val computable = binaryComputable[name]?.createType(range) ?: return null
    val pairLeft = folder.foldContextType(left)

    val isNonColumnType = DfComputingUtil.toTypeName(right) != DfColumnNameFromContextSupport.COLUMN_FQN
    val pairRight = if (isNonColumnType)
      Pair(null, DfComputingUtil.toScalaType(right)?.let { DfScalaTypesMapper((left as PsiElement).project).convertTo(it) })
    else
      folder.foldContextType(right)

    //if (pairLeft == null || pairRight == null) return computable.compute(null, null)

    val tpeLeft = (if (DfComputingUtil.isEmpty(pairLeft?.second)) folder.foldColumnType(left)  else pairLeft?.second) ?: UnknownType
    val nameLeft = pairLeft?.first
    val tpeRight = (if (DfComputingUtil.isEmpty(pairRight?.second)) folder.foldColumnType(right) else pairRight?.second) ?: UnknownType
    val nameRight = pairRight?.first

    return if (!DfComputingUtil.isEmpty(tpeLeft) && !DfComputingUtil.isEmpty(tpeRight))
      computable.compute(tpeLeft, tpeRight)
    else if (nameLeft != null && nameRight != null && DfComputingUtil.isEmpty(tpeLeft) && DfComputingUtil.isEmpty(tpeRight))
      object : OuterComputableType2(nameLeft, nameRight, computable.baseType) {
        override fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType = computable.compute(left, right)
      }
    else if (nameLeft != null && !DfComputingUtil.isEmpty(tpeRight))
      object : OuterComputableType1(nameLeft, computable.baseType) {
        override fun computeImpl(t: DfColumnType): DfColumnType = computable.compute(t, tpeRight)
      }
    else if (nameRight != null && !DfComputingUtil.isEmpty(tpeLeft))
      object : OuterComputableType1(nameRight, computable.baseType) {
        override fun computeImpl(t: DfColumnType): DfColumnType = computable.compute(tpeLeft, t)
      }
    else
      computable.compute(null, null)
  }

  companion object {
    val COMPUTED_COLUMN_TYPE = Key.create<DfComputingUtil.CachedValue<DfColumnType>>("Already computed DF type for column(expr)")

    private const val DF_SPARK_PACKAGE = "org.apache.spark"

    private val DF_COLUMN_TYPE_LIST = DfColumnTypesUtil.ALL_SIMPLE_TYPES.associateBy { it.jsonStructure }

    private val PARTICULAR_CALLS_LIST = listOf(
      DfFoldableTypeSupport.Companion.FromCallToParamTransit("cast", "to"),
      DfFoldableTypeSupport.Companion.FromCallToParamTransit("withExpr", "expr"),
      DfFoldableTypeSupport.Companion.FromCallToParamTransit("Column", "expr")
    ).associateBy { it.callName }

    private val PARTICULAR_TYPES_LIST = hashMapOf(
      Pair("org.apache.spark.sql.catalyst.expressions.Predicate", BooleanType),
      Pair("org.apache.spark.sql.catalyst.expressions.PartitionTransformExpression", IntegerType),
      Pair("org.apache.spark.sql.catalyst.expressions.ConcatWs", StringType),
      Pair("org.apache.spark.sql.catalyst.expressions.GetDateField", IntegerType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathBoolean", BooleanType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathShort", ShortType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathInt", IntegerType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathLong", LongType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathFloat", FloatType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathDouble", DoubleType),
      Pair("org.apache.spark.sql.catalyst.expressions.xml.XPathString", StringType),
    ).plus(DfColumnTypesUtil.ALL_TYPES.associateBy { "$DF_SPARK_PACKAGE.sql.types." + it.simpleName })

    private val idComputable = hashMapOf<String, DfColumnType>(
      Pair("between", BooleanType),
      Pair("isNaN", BooleanType),
      Pair("isNull", BooleanType),
      Pair("isNotNull", BooleanType),
      Pair("isin", BooleanType),
      Pair("isInCollection", BooleanType),
      Pair("like", BooleanType),
      Pair("rlike", BooleanType),
      Pair("contains", BooleanType),
      Pair("startsWith", BooleanType),
      Pair("endsWith", BooleanType),
    )

    private val unaryComputable = hashMapOf(
      Pair("sum", DfComputableTypes.IdNumericTypeCreator("sum")),
      Pair("-", DfComputableTypes.IdNumericTypeCreator("-")),
      Pair("!", DfComputableTypes.IdBooleanTypeCreator("!")),
      Pair("collect_set", DfComputableTypes.IdAnyTypeCreator),
      Pair("round", DfComputableTypes.IdAnyTypeCreator),
      Pair("to_timestamp", DfComputableTypes.IdTimestampTypeCreator)
    )

    private val binaryComputable = hashMapOf(
      Pair("\$minus", DfComputableTypes.UnionNumericTypeCreator("-")),
      Pair("minus", DfComputableTypes.UnionNumericTypeCreator("minus")),
      Pair("\$plus", DfComputableTypes.UnionNumericTypeCreator("+")),
      Pair("plus", DfComputableTypes.UnionNumericTypeCreator("plus")),
      Pair("\$times", DfComputableTypes.UnionNumericTypeCreator("*")),
      Pair("multiply", DfComputableTypes.UnionNumericTypeCreator("multiply")),
      Pair("\$div", DfComputableTypes.UnionNumericTypeCreator("/")),
      Pair("divide", DfComputableTypes.UnionNumericTypeCreator("divide")),
      Pair("\$percent", DfComputableTypes.UnionNumericTypeCreator("%")),
      Pair("mod", DfComputableTypes.UnionNumericTypeCreator("mod")),
      Pair("\$less", DfComputableTypes.UnionBooleanTypeCreator("<")),
      Pair("lt", DfComputableTypes.UnionBooleanTypeCreator("<")),
      Pair("\$greater", DfComputableTypes.UnionBooleanTypeCreator(">")),
      Pair("gt", DfComputableTypes.UnionBooleanTypeCreator(">")),
      Pair("\$less\$eq", DfComputableTypes.UnionBooleanTypeCreator("<=")),
      Pair("leq", DfComputableTypes.UnionBooleanTypeCreator("<=")),
      Pair("\$greater\$eq", DfComputableTypes.UnionBooleanTypeCreator(">=")),
      Pair("geq", DfComputableTypes.UnionBooleanTypeCreator(">=")),
      Pair("\$less\$eq\$greater", DfComputableTypes.UnionBooleanTypeCreator("<=>")),
      Pair("eqNullSafe", DfComputableTypes.UnionBooleanTypeCreator("eqNullSafe")),
      Pair("\$bang\$eq\$eq", DfComputableTypes.UnionBooleanTypeCreator("!==")),
      Pair("notEqual", DfComputableTypes.UnionBooleanTypeCreator("notEqual")),
      Pair("\$eq\$bang\$eq", DfComputableTypes.UnionBooleanTypeCreator("=!=")),
      Pair("\$eq\$eq\$eq", DfComputableTypes.UnionBooleanTypeCreator("===")),
      Pair("equalTo", DfComputableTypes.UnionBooleanTypeCreator("equalTo")),
      Pair("\$bar\$bar", DfComputableTypes.UnionBooleanTypeCreator("||")),
      Pair("or", DfComputableTypes.UnionBooleanTypeCreator("or")),
      Pair("\$amp\$amp", DfComputableTypes.UnionBooleanTypeCreator("&&")),
      Pair("and", DfComputableTypes.UnionBooleanTypeCreator("and")),
      Pair("to_timestamp", DfComputableTypes.IdDropRightTimestampCreator)
    )
  }
}