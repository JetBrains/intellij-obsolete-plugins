package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyTypedElement

object PysparkDataFrameBraceColumnCompletionProvider : PysparkDataFrameCompletionProvider() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val subscription: PySubscriptionExpression = parameters.originalPosition?.parentOfType<PySubscriptionExpression>() ?: return
    val sourceDataframeReference = subscription.firstChild as? PyTypedElement ?: return
    addSchemaCompletion(sourceDataframeReference, result)
  }
}