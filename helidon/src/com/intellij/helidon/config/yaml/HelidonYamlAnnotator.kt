// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.HelidonConfigFileAnnotator
import com.intellij.helidon.config.HelidonParametrizedConfigKey
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.yaml.YAMLHighlighter
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

internal class HelidonYamlAnnotator : HelidonConfigFileAnnotator() {
  private val PARAMETER_MARK_KEY: Key<Long> = Key.create("PARAMETER_MARK")

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is YAMLKeyValue) return

    val file = holder.currentAnnotationSession.file as? YAMLFile ?: return
    if (!hasHelidonLibrary(file.project) ||
        !isHelidonConfigFile(file)) {
      return
    }

    val yamlKey = element.key ?: return
    annotateKey(element, yamlKey, holder)

    val yamlValue = element.value
    if (yamlValue is YAMLScalar) {
      annotateValue(yamlValue, holder)
    }
    else if (yamlValue is YAMLSequence) {
      for (item in yamlValue.items) {
        val value = item.value as? YAMLScalar ?: continue
        annotateValue(value, holder)
      }
    }
  }

  override fun getPlaceholderTextAttributesKey(): TextAttributesKey = YAMLHighlighter.SCALAR_KEY

  private fun annotateKey(yamlKeyValue: YAMLKeyValue, yamlKey: PsiElement, holder: AnnotationHolder) {
    val markStamp = yamlKeyValue.getUserData(PARAMETER_MARK_KEY)
    if (markStamp == PsiModificationTracker.getInstance(yamlKeyValue.project).modificationCount) {
      // This is a parameter part of parametrized key marked during current highlighting pass.
      doAnnotateEnforced(holder, yamlKey.textRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES")
      return
    }

    val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(yamlKeyValue) ?: return
    val parametrizedConfigKey = HelidonParametrizedConfigKey.getParametrizedConfigKey(configKey.name) ?: return
    val keyName = ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue)
    val parameterRange = parametrizedConfigKey.getParameterRange(keyName) ?: return
    val parameter = parameterRange.substring(keyName)
    var parent = PsiTreeUtil.getParentOfType(yamlKeyValue, YAMLKeyValue::class.java)
    while (parent != null) {
      if (parameter == parent.keyText) {
        parent.putUserData(PARAMETER_MARK_KEY,
                           PsiModificationTracker.getInstance(yamlKeyValue.project).modificationCount)
        return
      }
      parent = PsiTreeUtil.getParentOfType(parent, YAMLKeyValue::class.java)
    }
  }
}