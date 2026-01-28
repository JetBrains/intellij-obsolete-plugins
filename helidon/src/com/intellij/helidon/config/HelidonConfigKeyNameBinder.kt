// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.ConfigKeyParts
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.microservices.jvm.config.RelaxedNames
import com.intellij.openapi.util.text.StringUtil

internal object HelidonConfigKeyNameBinder : MetaConfigKeyManager.ConfigKeyNameBinder {

  override fun bindsTo(key: MetaConfigKey, configKeyText: String): Boolean {
    if (!matchesFirstChar(key.name, configKeyText)) {
      return false
    }

    val uniformedConfigKeyText = toUniform(key.name, configKeyText)
    val exactMatch = key.name == uniformedConfigKeyText
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return exactMatch
    }
    if (key.isAccessType(*MetaConfigKey.AccessType.MAP_GROUP)) {
      return exactMatch || StringUtil.startsWith(uniformedConfigKeyText, key.name + ".")
    }
    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      if (exactMatch) {
        return true
      }
      if (!StringUtil.containsChar(uniformedConfigKeyText, '[')) {
        return false
      }
      var beforeIndexAccess = StringUtil.substringBefore(uniformedConfigKeyText, "[")!!
      beforeIndexAccess = StringUtil.trimEnd(beforeIndexAccess, '.')
      return key.name == beforeIndexAccess
    }
    throw IllegalArgumentException("unknown access type for $key")
  }

  override fun matchesPrefix(key: MetaConfigKey, prefixText: String): Boolean {
    return matchesFirstChar(key.name, prefixText) && StringUtil.startsWith(key.name, toUniform(key.name, prefixText))
  }

  override fun matchesPart(keyPart: String, text: String): Boolean {
    return keyPart == "*" || (matchesFirstChar(keyPart, text) && matches(keyPart, text))
  }

  override fun bindsToKeyProperty(key: MetaConfigKey,
                                  keyProperty: String?,
                                  configKeyText: String): String? {
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) return null
    val keyName = key.name

    // first character must match, even for RelaxedNames
    if (!matchesFirstChar(keyName, configKeyText)) return null
    val parts = ConfigKeyParts.splitToParts(key, configKeyText, true)
    return parts?.getKeyIndexIfMatches(keyName, keyProperty, this::matches)
  }

  private fun matchesFirstChar(keyName: String, configKeyText: String): Boolean {
    return configKeyText.isNotEmpty() && StringUtil.charsEqualIgnoreCase(keyName[0], configKeyText[0])
  }

  private fun toUniform(keyName: String, configKeyText: String): String {
    val uniformedConfigKeyText = RelaxedNames.camelCaseToHyphen(configKeyText)
    val parametrizedConfigKey = HelidonParametrizedConfigKey.getParametrizedConfigKey(keyName) ?: return uniformedConfigKeyText
    val parameterRange = parametrizedConfigKey.getParameterRange(uniformedConfigKeyText) ?: return uniformedConfigKeyText
    val result = uniformedConfigKeyText.substring(0, parameterRange.startOffset) + "*"
    if (parameterRange.endOffset == uniformedConfigKeyText.length) {
      return result
    }
    else {
      return result + uniformedConfigKeyText.substring(parameterRange.endOffset)
    }
  }

  private fun matches(keyName: String, configKeyText: String): Boolean = keyName == toUniform(keyName, configKeyText)
}