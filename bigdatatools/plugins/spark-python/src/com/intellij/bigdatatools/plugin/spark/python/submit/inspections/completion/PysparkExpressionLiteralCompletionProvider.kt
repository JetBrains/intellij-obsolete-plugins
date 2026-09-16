package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.completion

import com.intellij.bigdatatools.plugin.spark.python.submit.inspections.resolver.PySparkUtils
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.types.TypeEvalContext

object PysparkExpressionLiteralCompletionProvider : PysparkDataFrameCompletionProvider() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val original = parameters.originalPosition ?: return

    val evalContext = TypeEvalContext.codeCompletion(original.project, original.containingFile)
    val schema = PySparkUtils.resolveInvokeParentDf(original, evalContext) ?: return

    addSchemaToCompletion(schema, result)
  }
}