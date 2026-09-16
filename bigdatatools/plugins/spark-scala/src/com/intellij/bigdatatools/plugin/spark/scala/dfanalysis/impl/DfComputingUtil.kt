package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeContext
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.TypeWithAttachments
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.removeUserData
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.types.Context
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.TypePresentationContext
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable
import scala.collection.Seq

object DfComputingUtil {
  const val DF_COLUMN_TYPE_COMPUTING_DEPTH = 15
  const val DF_MAX_FILE_HOP_DEPTH = 3

  private const val MAX_DESC_LENGTH = 53
  private const val MAX_SUFFIX_LENGTH = 3

  const val APACHE_SPARK_PACKAGE = "org.apache.spark"

  val COMMON_DF_TYPE_KEY = Key.create<CachedValue<DfTypeContext>>("Inferred Type")
  val COMPUTED_DF_TYPE_KEY = Key.create<CachedValue<DfTypeSource>>("Already computed DF type")
  val COMMON_DF_MID_TYPE_KEY = Key.create<CachedValue<DfTypeSchema>>("Mid-computed data frame schema")

  data class CachedValue<T>(val v: T, val timestamp: Long)

  fun isEmpty(tpe: DfColumnType?): Boolean = tpe == null || tpe == UnknownType || tpe is TypeWithAttachments && tpe.base == UnknownType

  fun findTypeSchema(e: PsiElement): DfTypeSchema? {
    return getFromElement(COMMON_DF_MID_TYPE_KEY, e)
           ?: getFromElement(COMPUTED_DF_TYPE_KEY, e)?.schema()
           ?: getFromElement(COMMON_DF_TYPE_KEY, e)?.toSchema()
  }

  fun <T> saveToElement(key: Key<CachedValue<T>>, v: T, e: PsiElement) {
    e.putUserData(key, CachedValue(v, findModTracker(e).modificationCount))
  }

  fun <T> getFromElement(key: Key<CachedValue<T>>, e: PsiElement): T? {
    val cached = e.getUserData(key) ?: return null
    val currentTs = findModTracker(e).modificationCount

    if (cached.timestamp == currentTs) return cached.v

    e.removeUserData(key)
    return null
  }

  fun findContext(scalaPsi: ScalaPsiElement): DfTypeContext? =
    getFromElement(COMMON_DF_TYPE_KEY, scalaPsi as PsiElement)

  fun checkMethodCallFqn(call: ScMethodCall): Boolean =
    ((call.deepestInvokedExpr() as? PsiReference)?.resolve() as? ScFunctionDefinition)?.let { checkFunDefFqn(it) } == true

  fun checkFunDefFqn(funDef: ScFunctionDefinition): Boolean =
    funDef.containingClass()?.qualifiedName()?.startsWith(APACHE_SPARK_PACKAGE) == true

  fun callToMethod(call: ScMethodCall): ScFunctionDefinition? {
    val plainResolve = (call.deepestInvokedExpr() as? PsiReference)?.resolve()
    if (plainResolve == null) return null
    if (plainResolve is ScFunctionDefinition) return plainResolve

    return (call.deepestInvokedExpr() as? ScReferenceExpression)?.multiResolveScala(false)?.map {
      it.innerResolveResult()
    }?.filter {
      it.isDefined
    }?.mapNotNull {
      it.get().actualElement as? PsiElement // nope it is not
    }?.firstOrNull { it is ScFunctionDefinition } as? ScFunctionDefinition
  }

  fun callToName(call: ScExpression): String? {
    return when (call) {
      is ScMethodCall -> (call.effectiveInvokedExpr as? PsiElement)?.lastChild?.text
      is ScNewTemplateDefinition -> call.name
      else -> null
    }
  }

  fun isDataFrameType(element: PsiElement): Boolean {
    val tpb = element as? Typeable ?: return false
    val maybeType = tpb.type()
    if (maybeType.isLeft) return false

    val tpe = maybeType.right().get()
    val typeFqn = tpe.presentableText(TypePresentationContext.emptyContext(), Context.apply(element))

    return typeFqn == "sql.DataFrame"
  }

  fun psiElementToDescription(e: PsiElement): String = trimTextForDescription(if (e is PsiNamedElement) e.name ?: "" else e.text)

  fun psiElementToPlaceRange(e: PsiElement): TextRange = e.lastChild?.textRange ?: e.navigationElement?.textRange ?: e.textRange

  fun toTypeName(expr: Typeable): String? =
    toScalaType(expr)?.canonicalText()?.removeSuffix(".type")?.removePrefix("_root_.")

  fun toScalaType(expr: Typeable): ScType? {
    val maybeTpe = expr.type() ?: return null
    if (maybeTpe.isLeft) return null

    return maybeTpe.right().get()
  }

  fun groupByParameters(expr: ScExpression, paramNames: Set<String>): Map<String, List<ScExpression>> {
    val mmap = hashMapOf<String, MutableList<ScExpression>>()
    expr.matchedParameters().foreach {
      if (paramNames.isEmpty() || paramNames.contains(it._2().name())) mmap.getOrPut(it._2().name()) { mutableListOf() }.add(it._1())
    }

    return mmap
  }

  fun getParameterExpr(expr: ScExpression, paramName: String): ScExpression? =
    groupByParameters(expr, setOf(paramName))[paramName]?.firstOrNull()

  fun trimTextForDescription(src: String): String =
    StringUtil.shortenTextWithEllipsis(src, MAX_DESC_LENGTH, MAX_SUFFIX_LENGTH)

  fun <K, V> linkedHashMapOf(elems: Iterable<Pair<K, V>>): LinkedHashMap<K, V> {
    val map = LinkedHashMap<K, V>()
    map.putAll(elems)
    return map
  }

  fun <K, V> linkedHashMapOf(vararg e: Pair<K, V>): LinkedHashMap<K, V> = linkedHashMapOf(e.asList())

  //We need this method because right now Scala Plugin returns matchedParameters() for vararg in reverse order,
  //ad that breaks highlighting for such parameters
  fun <T> handleVarargParams(seq: Seq<T>): Seq<T> = seq

  fun <T> handleVarargParams(list: List<T>): List<T> = list

  fun <T> getVarargParam(list: List<T>?, idx: Int): T? = list?.get(idx)

  private fun findModTracker(e: PsiElement): ModificationTracker = BlockModificationTracker.apply(e)
}