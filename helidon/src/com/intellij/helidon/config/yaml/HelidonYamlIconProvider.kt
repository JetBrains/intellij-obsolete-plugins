// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.getHelidonConfigFileIcon
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.ide.IconProvider
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import javax.swing.Icon

internal class HelidonYamlIconProvider : IconProvider() {
  override fun getIcon(element: PsiElement, @Iconable.IconFlags flags: Int): Icon? {
    if (element is YAMLFile) {
      if (!hasHelidonLibrary(element.project)) return null

      return getHelidonConfigFileIcon(element)
    }
    else if (element is YAMLKeyValue) {
      val containingFile = element.getContainingFile()
      if (containingFile !is YAMLFile ||
          !hasHelidonLibrary(element.project) ||
          !isHelidonConfigFile(containingFile)) {
        return null
      }
      return MetaConfigKeyReference.getResolvedMetaConfigKey(element)?.presentation?.icon
    }
    return null
  }
}