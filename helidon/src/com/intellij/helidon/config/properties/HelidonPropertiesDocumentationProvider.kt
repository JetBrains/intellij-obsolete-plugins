// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.microservices.jvm.config.ConfigKeyDocumentationProviderBase
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.psi.PsiElement

internal class HelidonPropertiesDocumentationProvider : ConfigKeyDocumentationProviderBase() {

  override fun getConfigManager(): MetaConfigKeyManager = HelidonMetaConfigKeyManager.getInstance()

  override fun getConfigKey(configKeyElement: PsiElement): String? {
    if (configKeyElement !is PropertyImpl) return null

    val file = configKeyElement.containingFile
    if (file !is PropertiesFile || !isHelidonConfigFile(file)) return null

    return configKeyElement.key
  }
}