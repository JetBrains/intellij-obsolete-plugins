// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider
import com.intellij.lang.properties.psi.Property
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.microservices.jvm.config.MetaConfigKeyReference

internal class HelidonPropertiesImplicitPropertyUsageProvider : ImplicitPropertyUsageProvider {
  override fun isUsed(property: Property): Boolean {
    if (property !is PropertyImpl ||
        !hasHelidonLibrary(property.project) ||
        !isHelidonConfigFile(property.containingFile)) {
      return false
    }

    val propertyKey: PropertyKeyImpl = HelidonPropertiesUtils.getPropertyKey(property) ?: return false
    return MetaConfigKeyReference.getResolvedMetaConfigKey(propertyKey) != null
  }
}