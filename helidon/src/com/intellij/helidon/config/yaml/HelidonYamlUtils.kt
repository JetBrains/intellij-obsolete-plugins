// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

fun isInsideApplicationYamlFile(psiElement: PsiElement): Boolean {
  val containingFile = psiElement.containingFile ?: return false
  val originalFile = containingFile.originalFile
  return originalFile is YAMLFile && isHelidonConfigFile(originalFile)
}

fun getQualifiedConfigKeyName(yamlKeyValue: YAMLKeyValue?): String {
  return ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue) {
    val keyText = it.keyText
    if (keyText.contains('.')) {
      it.key?.text ?: keyText
    }
    else {
      keyText
    }
  }
}