// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLTokenTypes

private val VALUE_PATTERN = PlatformPatterns.psiElement(LeafPsiElement::class.java)
  .andOr(PlatformPatterns.psiElement().withElementType(YAMLElementTypes.SCALAR_VALUES)
           .andNot(PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(YAMLTokenTypes.INDENT))),
         PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(YAMLTokenTypes.COLON)))
  .with(APPLICATION_YAML_CONDITION)

/**
 * Suppress {@link JavaClassNameCompletionContributor} for value elements in Helidon YAML config files.
 */
internal class HelidonYamlClassNameCompletionSuppressor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, VALUE_PATTERN, object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(parameters: CompletionParameters,
                                  context: ProcessingContext,
                                  result: CompletionResultSet) {
        if (parameters.isExtendedCompletion) {
          result.stopHere()
        }
      }
    })
  }
}