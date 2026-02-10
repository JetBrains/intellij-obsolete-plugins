// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.microservices.jvm.config.ConfigFileSmartClassReferenceCompletionContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.YAMLFile

internal class HelidonYamlSmartClassReferenceCompletionContributor : ConfigFileSmartClassReferenceCompletionContributor() {
  override fun isMyFile(file: PsiFile): Boolean =file is YAMLFile && isHelidonConfigFile(file)

  // technically YAMLScalar (but not present if no value given at all)
  override fun isValueElement(position: PsiElement): Boolean = position is LeafPsiElement
}