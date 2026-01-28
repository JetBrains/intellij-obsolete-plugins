// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.helidon.config.HelidonMetaConfigKey
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.ConfigKeyParts
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.SmartList

internal class HelidonConfigKeyPathReferenceSet(private val element: PsiElement,
                                                private val configKey: HelidonMetaConfigKey,
                                                text: String, offset: Int) {
  val references: MutableList<PsiReference> = SmartList()

  init {
    assert(configKey.isAccessType(MetaConfigKey.AccessType.INDEXED, MetaConfigKey.AccessType.MAP,
                                  MetaConfigKey.AccessType.ENUM_MAP)) { configKey }
    parse(text, offset)
  }

  private fun parse(text: String, offset: Int) {
    if (configKey.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      val closingBracketIndex = text.indexOf("].")
      if (closingBracketIndex == -1) return

      val subKeyIndex = closingBracketIndex + 2
      val subKeyText = text.substring(subKeyIndex)
      parseSubKey(subKeyText, offset + subKeyIndex)
      return
    }

    val parts = ConfigKeyParts.splitToParts(configKey, text, false) ?: return
    val entryOffset = parts.configKey.length + 1
    if (entryOffset > text.length) {
      return
    }
    val entryText = text.substring(entryOffset)

    var separatorIndex = entryText.indexOf('.', parts.keyIndex?.length ?: 0)
    if (separatorIndex < 0) {
      separatorIndex = entryText.length
    }
    val entryKeyRange = TextRange.create(0, separatorIndex).shiftRight(offset + entryOffset)
    references.add(HelidonConfigKeyPathArbitraryEntryKeyReference(element, entryKeyRange))

    if (separatorIndex < entryText.length) {
      // shift after dot
      separatorIndex++
    }
    val subKeyText = entryText.substring(separatorIndex)
    parseSubKey(subKeyText, offset + entryOffset + separatorIndex)
  }

  private fun parseSubKey(text: String, offset: Int) {
    val subKeys = configKey.subKeys
    if (subKeys.isEmpty()) return

    var subKey: HelidonMetaConfigKey? = null
    if (text.isNotEmpty()) {
      subKey = subKeys.firstOrNull { it.isAccessType(MetaConfigKey.AccessType.NORMAL) && Comparing.equal(it.name, text, true) }
      if (subKey == null) {
        val module = ModuleUtilCore.findModuleForPsiElement(element)
        if (module != null) {
          val binder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module)
          subKey = subKeys.firstOrNull { binder.bindsTo(it, text) }
        }
      }
    }
    references.add(HelidonPropertySubKeyMetaConfigKeyReference(element, text, configKey, offset))
    if (subKey != null && !subKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      val subReferenceSet = HelidonConfigKeyPathReferenceSet(element, subKey, text, offset)
      references.addAll(subReferenceSet.references)
    }
  }
}