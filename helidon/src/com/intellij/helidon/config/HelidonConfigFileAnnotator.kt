// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.microservices.jvm.config.hints.HintReferenceBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.ContainerUtil

abstract class HelidonConfigFileAnnotator : Annotator {
  private val DEBUG_MODE = ApplicationManager.getApplication().isUnitTestMode

  protected abstract fun getPlaceholderTextAttributesKey(): TextAttributesKey

  protected fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val elementOffset = element.node.startOffset
    val references = element.references
    val highlightOnlyPlaceholders = ContainerUtil.findInstance(references, HelidonConfigPlaceholderReference::class.java) != null
    val annotatedOffsets: MutableSet<Int> = HashSet()
    for (reference in references) {
      var key: TextAttributesKey? = null
      if (highlightOnlyPlaceholders) {
        if (reference is HelidonConfigPlaceholderReference) {
          key = getPlaceholderTextAttributesKey()
        }
      }
      else {
        if (reference is JavaClassReference ||
            reference is PsiPackageReference) {
          if (reference.resolve() != null) {   // FQN references are injected by default in .properties
            key = DefaultLanguageHighlighterColors.CLASS_REFERENCE
          }
        }
        else if (reference is HintReferenceBase) {
          key = reference.textAttributesKey
        }
      }
      if (key != null) {
        val highlightTextRange = reference.rangeInElement.shiftRight(elementOffset)
        if (!annotatedOffsets.add(highlightTextRange.startOffset)) continue
        doAnnotate(holder, highlightTextRange, key)
      }
    }
  }

  protected fun doAnnotate(holder: AnnotationHolder,
                           range: TextRange,
                           key: TextAttributesKey) {
    if (range.isEmpty) return

    val annotationBuilder =
      if (DEBUG_MODE) {
        holder.newAnnotation(HighlightSeverity.INFORMATION, key.externalName)
      }
      else {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      }
    annotationBuilder.range(range).textAttributes(key).create()
  }

  @Suppress("SameParameterValue")
  protected fun doAnnotateEnforced(holder: AnnotationHolder,
                                   range: TextRange,
                                   key: SimpleTextAttributes,
                                   debugMessage: String) {
    if (range.isEmpty) return

    @Suppress("HardCodedStringLiteral")
    val message = if (DEBUG_MODE) debugMessage else null
    val annotationBuilder =
      if (message != null) {
        holder.newAnnotation(HighlightSeverity.INFORMATION, message)
      }
      else {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      }
    annotationBuilder.range(range).enforcedTextAttributes(key.toTextAttributes()).create()
  }
}