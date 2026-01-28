// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.HelidonMetaConfigKey
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.ConfigKeyParts
import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLPsiElement

internal class HelidonYamlKeyMetaConfigKeyReference(yamlKeyValue: YAMLKeyValue) :
  MetaConfigKeyReference<YAMLKeyValue>(HelidonMetaConfigKeyManager.getInstance(), yamlKeyValue, "") {

  val keyData: HelidonYamlKeyData by lazy { resolveKey() }

  init {
    rangeInElement = TextRange.allOf(yamlKeyValue.keyText).shiftRight(ElementManipulators.getOffsetInElement(yamlKeyValue))
  }

  override fun getResolvedKey(): MetaConfigKey? = keyData.key

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    if (keyData.key == null) return ResolveResult.EMPTY_ARRAY

    return PsiElementResolveResult.createResults(SmartList(keyData.key!!.declaration))
  }

  override fun getReferenceDisplayText(): String = ConfigYamlUtils.getReferenceDisplayText(element)

  private fun resolveKey(): HelidonYamlKeyData {
    val builder = HelidonYamlKeyDataBuilder(collectParents())
    val topKeyValue = builder.parents.firstOrNull()
    if (topKeyValue == null || !topKeyValue.keyText.startsWith('%')) {
      builder.root = PsiTreeUtil.getParentOfType(element, YAMLDocument::class.java) ?: error(
        "Yaml document not found for $element in ${element.containingFile}")
    }
    else {
      builder.root = topKeyValue
      builder.parents = builder.parents.subList(1, builder.parents.size)
    }

    val qualifiedConfigKeyName = getQualifiedConfigKeyName(element)

    if (qualifiedConfigKeyName.endsWith(".~")) {
      builder.keyText = qualifiedConfigKeyName.substring(0, qualifiedConfigKeyName.length - 2)
      builder.requireNormal = true
    }
    else {
      builder.keyText = qualifiedConfigKeyName
      builder.requireNormal = false
    }

    val key = getAllKeys(builder.keyText).firstOrNull()
    if (key != null) {
      adjustKey(builder, key as HelidonMetaConfigKey)
    }

    return builder.build()
  }

  private fun collectParents(): List<YAMLKeyValue> {
    val result = ArrayList<YAMLKeyValue>()
    var parent: YAMLKeyValue? = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
    while (parent != null) {
      result.add(parent)
      parent = PsiTreeUtil.getParentOfType(parent, YAMLKeyValue::class.java)
    }
    result.reverse()
    return result
  }

  private fun adjustKey(builder: HelidonYamlKeyDataBuilder, configKey: HelidonMetaConfigKey) {
    builder.key = configKey

    if (configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) return

    if (configKey.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      val closingBracketIndex = builder.keyText.indexOf("].")
      if (closingBracketIndex == -1) {
        if (builder.requireNormal) {
          builder.key = null
        }
        return
      }

      val subKeyIndex = closingBracketIndex + 2
      builder.keyText = builder.keyText.substring(subKeyIndex)
      parseSubKey(builder, configKey)
    }

    val parts = ConfigKeyParts.splitToParts(configKey, builder.keyText, false)
    if (parts == null) {
      if (builder.requireNormal) {
        builder.key = null
      }
      return
    }

    val entryOffset = parts.configKey.length + 1
    if (entryOffset > builder.keyText.length) {
      if (builder.requireNormal) {
        builder.key = null
      }
      return
    }
    builder.keyText = builder.keyText.substring(entryOffset)

    var separatorIndex = builder.keyText.indexOf('.', parts.keyIndex?.length ?: 0)
    if (separatorIndex < 0) {
      if (builder.requireNormal) {
        builder.key = null
        builder.parentKey = configKey
        return
      }
      else {
        builder.pathType = ConfigKeyPathReference.PathType.ARBITRARY_ENTRY_KEY
        return
      }
    }

    if (separatorIndex < builder.keyText.length) {
      // shift after dot
      separatorIndex++
    }
    builder.keyText = builder.keyText.substring(separatorIndex)
    return parseSubKey(builder, configKey)
  }

  private fun parseSubKey(builder: HelidonYamlKeyDataBuilder, parent: HelidonMetaConfigKey) {
    builder.parentKey = parent

    val parentKeyNameSeparators = parent.name.count { it == '.' }
    var index = 0
    while (index < parentKeyNameSeparators) {
      if (builder.parents.size > index) {
        val rootKeyValue = builder.parents[index]
        index += 1 + rootKeyValue.keyText.count { it == '.' }
      }
      else {
        break
      }
    }
    index++
    if (builder.parents.size > index) {
      builder.root = builder.parents[index]
      builder.parents = builder.parents.subList(index + 1, builder.parents.size)
    }

    val subKeys = parent.subKeys
    if (subKeys.isEmpty()) {
      builder.key = null
      return
    }

    var subKey: HelidonMetaConfigKey? = null
    if (builder.keyText.isNotEmpty()) {
      subKey = subKeys.firstOrNull { it.isAccessType(MetaConfigKey.AccessType.NORMAL) && Comparing.equal(it.name, builder.keyText, true) }
      if (subKey == null) {
        val module = ModuleUtilCore.findModuleForPsiElement(element)
        if (module != null) {
          val binder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module)
          subKey = subKeys.firstOrNull { binder.bindsTo(it, builder.keyText) }
        }
      }
    }
    if (subKey == null) {
      builder.key = null
      return
    }
    adjustKey(builder, subKey)
  }

  data class HelidonYamlKeyData(val key: HelidonMetaConfigKey?,
                                val parentKey: HelidonMetaConfigKey?,
                                val root: YAMLPsiElement,
                                val keyText: String,
                                val pathType: ConfigKeyPathReference.PathType? = null)

  private class HelidonYamlKeyDataBuilder(var parents: List<YAMLKeyValue>) {
    var key: HelidonMetaConfigKey? = null
    var parentKey: HelidonMetaConfigKey? = null
    var root: YAMLPsiElement? = null
    var keyText: String = ""
    var pathType: ConfigKeyPathReference.PathType? = null

    var requireNormal: Boolean = false

    fun build(): HelidonYamlKeyData = HelidonYamlKeyData(key, parentKey, root ?: error("Key data root not set"), keyText, pathType)
  }
}