// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.HelidonConfigFileAnnotator
import com.intellij.helidon.config.HelidonParametrizedConfigKey
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.properties.PropertiesHighlighter
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.microservices.jvm.config.ConfigKeyPathReference.PathType.*
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.beanProperties.BeanPropertyElement
import com.intellij.ui.SimpleTextAttributes

internal class HelidonPropertiesAnnotator : HelidonConfigFileAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is PropertyValueImpl && element !is PropertyKeyImpl) return

    val file = holder.currentAnnotationSession.file
    if (file !is PropertiesFile) return

    if (!hasHelidonLibrary((file as PropertiesFile).project) ||
        !isHelidonConfigFile(file)) {
      return
    }
    if (element is PropertyValueImpl) {
      annotateValue(element, holder)
    }
    else if (element is PropertyKeyImpl) {
      annotateKey(element, holder)
    }
  }

  override fun getPlaceholderTextAttributesKey(): TextAttributesKey =
    PropertiesHighlighter.PropertiesComponent.PROPERTY_KEY.textAttributesKey

  private fun annotateKey(element: PropertyKeyImpl, holder: AnnotationHolder) {
    val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(element) ?: return
    val keyText = element.text
    val elementStartOffset = element.node.startOffset
    annotateIndexAccessExpressions(holder, keyText, configKey, elementStartOffset)
    annotateParameter(holder, keyText, configKey, elementStartOffset)
    if (configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return
    }

    for (psiReference in element.references) {
      if (psiReference !is ConfigKeyPathReference) continue

      val referenceRange = psiReference.rangeInElement.shiftRight(elementStartOffset)
      when (psiReference.pathType) {
        ENUM -> doAnnotate(holder, referenceRange, DefaultLanguageHighlighterColors.CONSTANT)
        BEAN_PROPERTY -> {
          doAnnotate(holder, referenceRange, DefaultLanguageHighlighterColors.INSTANCE_METHOD)
          val resolve = psiReference.resolve()
          if (resolve is BeanPropertyElement && resolve.method.isDeprecated) {
            doAnnotate(holder, referenceRange, CodeInsightColors.DEPRECATED_ATTRIBUTES)
          }
        }
        ARBITRARY_ENTRY_KEY -> doAnnotateEnforced(holder, referenceRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES,
                                                  "REGULAR_ITALIC_ATTRIBUTES")
      }
    }

    // highlight map key for config key with hints (e.g. logger.levels.<JavaClassReferenceSet>)
    // when ConfigKeyPathArbitraryEntryKeyReference is not provided in order not to overlap references
    if (configKey.isAccessType(MetaConfigKey.AccessType.MAP) &&
        configKey.keyItemHint !== MetaConfigKey.ItemHint.NONE) {
      var configKeyNameLength = -1
      for (reference in element.references) {
        if (reference is MetaConfigKeyReference<*>) {
          configKeyNameLength = reference.getRangeInElement().endOffset
          break
        }
      }
      assert(configKeyNameLength != -1) { keyText }
      val endOffset = keyText.length - configKeyNameLength - if (StringUtil.endsWithChar(keyText, ']')) 2 else 1
      val genericKeyRange = TextRange.from(configKeyNameLength + 1, endOffset).shiftRight(elementStartOffset)
      doAnnotateEnforced(holder, genericKeyRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES")
    }
  }

  private fun annotateParameter(holder: AnnotationHolder,
                                text: String,
                                configKey: MetaConfigKey,
                                elementStartOffset: Int) {
    val parametrizedConfigKey = HelidonParametrizedConfigKey.getParametrizedConfigKey(configKey.name) ?: return
    val parameterRange = parametrizedConfigKey.getParameterRange(text)?.shiftRight(elementStartOffset) ?: return
    doAnnotateEnforced(holder, parameterRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES")
  }

  private fun annotateIndexAccessExpressions(holder: AnnotationHolder,
                                             text: String,
                                             configKey: MetaConfigKey,
                                             elementStartOffset: Int) {
    object : IndexAccessTextProcessor(text, configKey) {
      override fun onMissingClosingBracket(startIdx: Int) {}
      override fun onMissingIndexValue(startIdx: Int) {}
      override fun onBracket(startIdx: Int) {
        doAnnotate(holder,
                   TextRange.from(startIdx, 1).shiftRight(elementStartOffset),
                   DefaultLanguageHighlighterColors.BRACKETS)
      }

      override fun onIndexValue(indexValueRange: TextRange) {
        doAnnotate(holder, indexValueRange.shiftRight(elementStartOffset),
                   DefaultLanguageHighlighterColors.NUMBER)
      }

      override fun onIndexValueNotInteger(indexValueRange: TextRange) {}
    }.process()
  }
}