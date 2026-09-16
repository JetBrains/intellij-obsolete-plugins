package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.completion

import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticPython
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.model.PySparkSchemaInfo
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkConsts
import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkDataFrameResolver
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.TypeEvalContext

abstract class PysparkDataFrameCompletionProvider : CompletionProvider<CompletionParameters>() {
  protected fun addSchemaCompletion(sourceDataframeReference: PyTypedElement, result: CompletionResultSet) {
    val resolver = createResolver(sourceDataframeReference)

    val typedElement = resolver.context.getType(sourceDataframeReference) as? PyClassType
    if (typedElement?.classQName != PySparkConsts.DATAFRAME_CLASS_NAME)
      return

    val schema = resolver.resolve(sourceDataframeReference) ?: return
    addSchemaToCompletion(schema, result)
  }

  protected fun addSchemaToCompletion(schema: PySparkSchemaInfo, result: CompletionResultSet) {
    SparkStatisticPython.logCompletion(schema.schemaColumns.size, isPartial = schema.isPartial)
    schema.schemaColumns.forEach { columnName ->
      val lookupElement = LookupElementBuilder.create(columnName)
        .withPresentableText(columnName)
        .withIcon(BigdatatoolsSparkMonitoringIcons.Spark)
        .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)

      result.addElement(PrioritizedLookupElement.withPriority(lookupElement, 1000500.0))
    }
  }

  protected fun createResolver(sourceDataframeReference: PsiElement): PySparkDataFrameResolver {
    val evalContext = TypeEvalContext.codeCompletion(sourceDataframeReference.project, sourceDataframeReference.containingFile)
    val resolver = PySparkDataFrameResolver(evalContext)
    return resolver
  }
}