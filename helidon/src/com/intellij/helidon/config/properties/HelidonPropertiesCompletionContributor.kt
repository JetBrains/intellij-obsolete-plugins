// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.helidon.config.HelidonParametrizedConfigKey
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.psi.PropertiesFile

internal class HelidonPropertiesCompletionContributor : CompletionContributor() {

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    val psiFile = parameters.originalFile
    if (psiFile !is PropertiesFile ||
        !hasHelidonLibrary((psiFile as PropertiesFile).project) ||
        !isHelidonConfigFile(psiFile)) {
      return
    }

    val reference = parameters.position.containingFile.findReferenceAt(parameters.offset)
    if (reference is HelidonPropertyKeyMetaConfigKeyReference) {
      val resultWithMatcher = result.withPrefixMatcher(ParametrizedKeyPrefixMatcher(result.prefixMatcher))
      resultWithMatcher.runRemainingContributors(parameters, true)
    }
  }

  private class ParametrizedKeyPrefixMatcher(prefix: String, delegate: PrefixMatcher) : PrefixMatcher(prefix) {
    constructor(delegate: PrefixMatcher) : this(delegate.prefix, delegate)
    private val delegate = delegate.cloneWithPrefix(prefix)

    override fun prefixMatches(element: LookupElement): Boolean {
      for (s in element.allLookupStrings) {
        val parametrizedConfigKey = HelidonParametrizedConfigKey.getParametrizedConfigKey(s!!)
        val parameterRange = parametrizedConfigKey?.getParameterRange(prefix)
        if (parameterRange != null) {
          val parameter = parameterRange.substring(prefix)
          val convertedName = parametrizedConfigKey.prefix +
                              HelidonParametrizedConfigKey.CONFIG_KEY_SEPARATOR +
                              parameter +
                              HelidonParametrizedConfigKey.CONFIG_KEY_SEPARATOR +
                              parametrizedConfigKey.suffix
          if (delegate.prefixMatches(convertedName)) {
            element.putUserData(HelidonPropertyKeyMetaConfigKeyReference.PARAMETER_KEY, parameter)
            return true
          }
        }
        else if (delegate.prefixMatches(s)) {
          return true
        }
      }
      return false
    }

    override fun prefixMatches(name: String): Boolean = delegate.prefixMatches(name)

    override fun cloneWithPrefix(prefix: String): PrefixMatcher {
      return if (prefix == myPrefix) this else ParametrizedKeyPrefixMatcher(prefix, delegate)
    }
  }
}