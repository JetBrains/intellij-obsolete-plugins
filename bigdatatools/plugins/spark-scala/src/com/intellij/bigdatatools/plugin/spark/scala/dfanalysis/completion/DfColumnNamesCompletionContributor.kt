package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.completion

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfCompletionUtils
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.scala.lang.psi.api.expr.MethodInvocation
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScArgumentExprList
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScInterpolatedStringLiteralImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScStringLiteralImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.literals.ScSymbolLiteralImpl

class DfColumnNamesCompletionContributor : CompletionContributor() {
  init {
    if (DfCompletionUtils.isEnabled()) {
      extendScala(ScInterpolatedStringLiteralImpl::class.java)
      extendScala(ScStringLiteralImpl::class.java)
      extendScala(ScSymbolLiteralImpl::class.java)
    }
  }

  private fun extendScala(clazz: Class<out PsiElement>) {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().inside(clazz), DfAlreadyComputedCompletionProvider)
  }
}

object DfAlreadyComputedCompletionProvider : CompletionProvider<CompletionParameters>() {
  private const val MAX_DEPTH = 3

  private fun getTopElement(method: ScMethodCall): PsiElement {
    var element = (method as PsiElement).parent
    while (element is ScArgumentExprList) {
      element = element.parent
    }
    return element
  }

  // This could help us in changed calls like
  private fun findSchemaInChildren(method: ScMethodCall): DfTypeSchema? {
    var firstChild: PsiElement = method
    var counter = 0
    do {
      firstChild = firstChild.firstChild ?: return null
      val schema = DfComputingUtil.findTypeSchema(firstChild)
      if (schema != null && !schema.map.isEmpty()) {
        return schema
      }
      counter++
    }
    while (counter < 10)

    return null
  }

  private fun getSchema(contextElement: PsiElement): DfTypeSchema? {

    if (contextElement is ScMethodCall) {

      // We have chained call like  Seq().toDF().select("_)
      val schemaInChildren = findSchemaInChildren(contextElement)
      if (schemaInChildren != null) {
        return schemaInChildren
      }

      // We are calling completion on method parameter. For example in select("_") or in more complex case like select(col("_"))
      val elt = getTopElement(contextElement)

      val invoked = if (elt is MethodInvocation) {
        (elt.invokedExpr as PsiElement).firstChild
      }
      else {
        elt.firstChild
      }

      val schema = DfComputingUtil.findTypeSchema(invoked)

      if (schema != null) {
        return schema
      }
    }

    var schema = DfComputingUtil.findTypeSchema(contextElement)

    // This is a hack, but the original version does not work.
    if ((schema == null || schema.map.isEmpty()) && contextElement is ScMethodCall) {
      val left = (contextElement.deepestInvokedExpr() as PsiElement).firstChild
      schema = DfComputingUtil.findTypeSchema(left)
    }

    return schema
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {

    //To handle the case when context is ReferenceExpr and we need ScMethodCall.
    val originalPosition = parameters.originalPosition ?: return
    val contextElement = findContext(originalPosition)

    val schema = getSchema(contextElement) ?: return

    val lookupElements = mutableListOf<LookupElement>()

    schema.map.keys.forEachIndexed { index, columnName ->
      val columnType = schema.map[columnName]
      val lookupElementBuilder = LookupElementBuilder.create(columnName)
        .withPresentableText(columnName)
        .withIcon(BigdatatoolsSparkMonitoringIcons.Spark)

      if (columnType != null) {
        lookupElementBuilder.withTypeText(columnType.simpleName)
      }

      val lookupElement = lookupElementBuilder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
      lookupElements += PrioritizedLookupElement.withPriority(lookupElement, 1000500.0 - index)
    }

    if (lookupElements.isNotEmpty()) {
      SparkStatisticScala.logCompletion(lookupElements.size, isPartial = false)
      result.addAllElements(lookupElements)
      result.stopHere()
    }
  }

  private fun findContext(p: PsiElement): PsiElement {
    var d = 0
    var c = p

    while (d < MAX_DEPTH) {
      d--
      c = c.context ?: return c
      if (c is ScMethodCall) return c
    }

    return c
  }
}