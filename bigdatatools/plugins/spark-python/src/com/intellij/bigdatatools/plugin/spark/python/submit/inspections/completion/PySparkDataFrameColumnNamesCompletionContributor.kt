package com.intellij.bigdatatools.plugin.spark.python.submit.inspections.completion

import com.intellij.bigdatatools.plugin.spark.python.submit.PySparkRegistry
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyStringElement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PySubscriptionExpression

class PySparkDataFrameColumnNamesCompletionContributor : CompletionContributor() {
  init {
    if (PySparkRegistry.ENABlE_PYSPARK_COMPLETION) {
      val dotColumnPattern = PlatformPatterns.psiElement()
        .inside(PyExpression::class.java)
        .withElementType(PyTokenTypes.IDENTIFIER)
      extend(CompletionType.BASIC, dotColumnPattern, PysparkDataFrameIdentifierCompletionProvider)

      val expressionColumnPattern = PlatformPatterns.psiElement()
        .inside(PyCallExpression::class.java)
        .inside(PyStringLiteralExpression::class.java)
      extend(CompletionType.BASIC, expressionColumnPattern, PysparkExpressionLiteralCompletionProvider)

      val subscriptionPattern = PlatformPatterns.psiElement(PyStringElement::class.java).inside(PySubscriptionExpression::class.java)
      extend(CompletionType.BASIC, subscriptionPattern, PysparkDataFrameBraceColumnCompletionProvider)
    }
  }
}