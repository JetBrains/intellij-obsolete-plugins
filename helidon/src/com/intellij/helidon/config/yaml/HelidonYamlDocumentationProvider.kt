// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.microservices.jvm.config.yaml.documentation.AbstractYamlConfigKeyDocumentationProvider
import com.intellij.psi.PsiElement

internal class HelidonYamlDocumentationProvider : AbstractYamlConfigKeyDocumentationProvider() {
  override fun getConfigManager(): MetaConfigKeyManager = HelidonMetaConfigKeyManager.getInstance()

  override fun isInsideYamlConfigurationFile(configKeyElement: PsiElement): Boolean =
    isInsideApplicationYamlFile(configKeyElement)
}