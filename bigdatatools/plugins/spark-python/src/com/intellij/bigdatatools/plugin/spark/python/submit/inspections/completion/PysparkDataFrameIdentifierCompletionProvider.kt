package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyTypedElement

object PysparkDataFrameIdentifierCompletionProvider : PysparkDataFrameCompletionProvider() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val sourceDataframeReference = parameters.position.parent.firstChild as? PyTypedElement ?: return
    addSchemaCompletion(sourceDataframeReference, result)
  }
}