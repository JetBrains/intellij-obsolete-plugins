// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.codeInspection.unsorted.AlphaUnsortedPropertiesFileInspectionSuppressor
import com.intellij.lang.properties.psi.PropertiesFile

internal class HelidonAlphaUnsortedPropertiesFileInspectionSuppressor : AlphaUnsortedPropertiesFileInspectionSuppressor {

  override fun suppressInspectionFor(propertiesFile: PropertiesFile): Boolean {
    return hasHelidonLibrary(propertiesFile.project) &&
           isHelidonConfigFile(propertiesFile.containingFile)
  }
}