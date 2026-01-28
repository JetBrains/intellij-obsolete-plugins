// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.microservices.jvm.config.ConfigFileSmartClassReferenceCompletionContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

internal class HelidonPropertiesSmartClassReferenceCompletionContributor : ConfigFileSmartClassReferenceCompletionContributor() {
  override fun isMyFile(file: PsiFile): Boolean = file is PropertiesFile && isHelidonConfigFile(file)

  override fun isValueElement(position: PsiElement): Boolean = position is PropertyValueImpl
}