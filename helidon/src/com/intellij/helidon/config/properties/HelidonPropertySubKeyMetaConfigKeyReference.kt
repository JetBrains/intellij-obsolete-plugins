// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.HelidonMetaConfigKey
import com.intellij.helidon.config.HelidonMetaConfigSubKeyManager
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.microservices.jvm.config.ConfigKeyParts
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

internal class HelidonPropertySubKeyMetaConfigKeyReference(element: PsiElement,
                                                           text: String,
                                                           parent: HelidonMetaConfigKey,
                                                           private val offset: Int) :
  MetaConfigKeyReference<PsiElement?>(HelidonMetaConfigSubKeyManager(parent), element, text) {

  override fun calculateDefaultRangeInElement(): TextRange {
    var defaultRange = super.calculateDefaultRangeInElement()
    defaultRange = TextRange.create(defaultRange.startOffset + offset, defaultRange.endOffset)
    val keyText = defaultRange.substring(element.text)

    // set reference range to resolved key's name range for map-type, e.g. logging.level.[package.name.here]
    val configKey = resolvedKey
    if (configKey != null && !configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      val parts = ConfigKeyParts.splitToParts(configKey, keyText, false)
      if (parts != null) {
        return TextRange.allOf(parts.configKey).shiftRight(defaultRange.startOffset)
      }
    }
    return defaultRange
  }

  override fun getReferenceDisplayText(): String = element.text

  override fun getVariants(): Array<Any> {
    val prefix = element.text.subSequence(super.calculateDefaultRangeInElement().startOffset, offset)
    val existingProperties = (element.containingFile as? PropertiesFile)?.properties ?: emptyList()
    val existingKeys = existingProperties
      .mapNotNull { it.key }
      .filter { it.startsWith(prefix) }
      .map { it.substring(prefix.length) }
      .toSet()
    return HelidonPropertiesUtils.getMetaConfigKeyVariants(element, configKeyManager, existingKeys)
  }
}