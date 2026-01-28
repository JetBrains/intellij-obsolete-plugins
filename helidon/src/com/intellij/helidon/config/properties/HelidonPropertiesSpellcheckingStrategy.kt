// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

/**
 * Suppress spellchecking for keys in `application.properties`.
 */
internal class HelidonPropertiesSpellcheckingStrategy : SpellcheckingStrategy(), DumbAware {
  override fun getTokenizer(element: PsiElement): Tokenizer<*> = EMPTY_TOKENIZER

  override fun isMyContext(element: PsiElement): Boolean {
    if (element !is PropertyKeyImpl) return false

    if (!hasHelidonLibrary(element.project)) return false

    val file = element.getContainingFile()
    return file is PropertiesFile && isHelidonConfigFile(file)
  }
}
