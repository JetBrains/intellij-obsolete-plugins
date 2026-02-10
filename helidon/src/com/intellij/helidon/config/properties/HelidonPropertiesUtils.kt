// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.microservices.jvm.config.properties.AutoPopupTailTypes
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.toArray

internal object HelidonPropertiesUtils {

  fun getPropertyKey(property: PropertyImpl): PropertyKeyImpl? {
    val keyNode = property.keyNode ?: return null
    val keyElement = keyNode.psi
    return if (keyElement is PropertyKeyImpl) keyElement else null
  }

  fun getPropertyValue(property: PropertyImpl): PropertyValueImpl? {
    val valueNode = property.valueNode ?: return null
    val valueElement = valueNode.psi
    return if (valueElement is PropertyValueImpl) valueElement else null
  }

  fun getMetaConfigKeyVariants(element: PsiElement, configKeyManager: MetaConfigKeyManager, existingKeys: Set<String>) : Array<Any> {
    val delimiterChar = PropertiesCodeStyleSettings.getInstance(element.project).delimiter
    val defaultDelimiterType = AutoPopupTailTypes.charType(delimiterChar)
    val configKeys = configKeyManager.getAllMetaConfigKeys(element)
    val result: MutableList<LookupElement> = ArrayList(configKeys.size)
    for (configKey in configKeys) {
      val name = configKey.name
      if (existingKeys.contains(name)) continue

      val builder = configKey.presentation.lookupElement
      val lookupElementRef = Ref<LookupElement>()
      val tailTypeDecorator = TailTypeDecorator.withTail(builder, defaultDelimiterType)
      val lookupElement = configKey.presentation.tuneLookupElement(tailTypeDecorator)
      lookupElementRef.set(lookupElement)
      result.add(lookupElement)
    }
    return result.toArray(ArrayUtil.EMPTY_OBJECT_ARRAY)
  }
}