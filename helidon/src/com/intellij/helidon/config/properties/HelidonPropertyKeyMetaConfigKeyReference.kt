// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.Property
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings
import com.intellij.microservices.jvm.config.ConfigKeyParts
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.properties.AutoPopupTailTypes
import com.intellij.microservices.jvm.config.properties.AutoPopupTailTypes.dotType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.toArray

internal class HelidonPropertyKeyMetaConfigKeyReference(element: PsiElement?, private val property: Property)
  : MetaConfigKeyReference<PsiElement?>(HelidonMetaConfigKeyManager.getInstance(), element, property.name) {

  override fun calculateDefaultRangeInElement(): TextRange {
    val defaultRange = super.calculateDefaultRangeInElement()
    // set reference range to resolved key's name range for map-type, e.g. logging.level.[package.name.here]
    val configKey = resolvedKey
    if (configKey != null && !configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      val keyText = defaultRange.substring(myElement!!.text)
      val parts = ConfigKeyParts.splitToParts(configKey, keyText, false)
      if (parts != null) {
        return TextRange.allOf(parts.configKey)
      }
    }
    return defaultRange
  }

  override fun getReferenceDisplayText(): String = property.text

  override fun getVariants(): Array<Any> {
    val existingKeys: Set<String?> = ContainerUtil.map2Set(property.propertiesFile.properties) { obj: IProperty -> obj.key }
    val delimiterChar = PropertiesCodeStyleSettings.getInstance(myElement!!.project).delimiter
    val defaultDelimiterType = AutoPopupTailTypes.charType(delimiterChar)
    val configKeys = configKeyManager.getAllMetaConfigKeys(element)
    val result: MutableList<LookupElement> = ArrayList(configKeys.size)
    for (configKey in configKeys) {
      val name = configKey.name
      if (existingKeys.contains(name)) continue

      val builder = configKey.presentation.lookupElement
      var tailType = if (configKey.isAccessType(*MetaConfigKey.AccessType.MAP_GROUP)) dotType() else defaultDelimiterType
      val lookupElementRef = Ref<LookupElement>()
      if (configKey.name.contains(".*.")) {
        val delegateTailType = tailType
        tailType = object : TailType() {
          override fun processTail(editor: Editor, tailOffset: Int): Int {
            val delegateTail = delegateTailType.processTail(editor, tailOffset)
            val document = editor.document
            val i = configKey.name.indexOf(".*.")
            val start = tailOffset - configKey.name.length + i + 1
            val replacement = lookupElementRef.get().getUserData(PARAMETER_KEY) ?: ""
            document.replaceString(start, start + 1, replacement)
            if (replacement.isEmpty()) {
              val model = editor.caretModel
              model.moveToOffset(start)
              return start
            }
            else {
              return delegateTail + replacement.length - 1
            }
          }
        }
      }
      val tailTypeDecorator = TailTypeDecorator.withTail(builder, tailType)
      val lookupElement = configKey.presentation.tuneLookupElement(tailTypeDecorator)
      lookupElementRef.set(lookupElement)
      result.add(lookupElement)
    }
    return result.toArray(ArrayUtil.EMPTY_OBJECT_ARRAY)
  }

  companion object {
    internal val PARAMETER_KEY: Key<String> = Key.create("PARAMETER_KEY")
  }
}