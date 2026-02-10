// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil

class HelidonParametrizedConfigKey private constructor(val prefix: String, val suffix: String) {
  private val prefixSeparatorsCount = StringUtil.countChars(prefix, CONFIG_KEY_SEPARATOR)

  fun getParameterRange(configKeyText: String): TextRange? {
    var configKeyTextPrefixIndex = -1
    for (i in 0 until prefixSeparatorsCount + 1) {
      configKeyTextPrefixIndex = StringUtil.indexOf(configKeyText, CONFIG_KEY_SEPARATOR, configKeyTextPrefixIndex + 1)
      if (configKeyTextPrefixIndex < 0) return null
    }
    var parameterEndIndex = StringUtil.indexOf(configKeyText, CONFIG_KEY_SEPARATOR, configKeyTextPrefixIndex + 1)
    if (parameterEndIndex < 0) {
      parameterEndIndex = configKeyText.length
    }

    return TextRange.create(configKeyTextPrefixIndex + 1, parameterEndIndex)
  }

  companion object {
    const val CONFIG_KEY_SEPARATOR = '.'
    private const val CONFIG_KEY_PARAMETER_PART = ".*."

    fun getParametrizedConfigKey(configKeyName: String): HelidonParametrizedConfigKey? {
      val parameterPartIndex = configKeyName.indexOf(CONFIG_KEY_PARAMETER_PART)
      if (parameterPartIndex <= 0) return null

      return HelidonParametrizedConfigKey(configKeyName.substring(0, parameterPartIndex),
                                          configKeyName.substring(parameterPartIndex + CONFIG_KEY_PARAMETER_PART.length))
    }
  }
}