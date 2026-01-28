// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.ide.IconProvider
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.psi.PsiElement
import javax.swing.Icon

internal class HelidonIconProvider : IconProvider() {
  override fun getIcon(element: PsiElement, flags: Int): Icon? {

    if (element is PropertyImpl) {
      if (!hasHelidonLibrary(element.project)) return null

      if (isHelidonConfigFile(element.containingFile)) {
        val keyNode = element.keyNode ?: return null
        val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(keyNode.psi)
        if (configKey != null) {
          return configKey.presentation.icon
        }
      }
      return null
    }

    if (element is PropertiesFile) {
      if (!hasHelidonLibrary((element as PropertiesFile).project)) return null

      return getHelidonConfigFileIcon((element as PropertiesFile).containingFile)
    }

    return null
  }
}