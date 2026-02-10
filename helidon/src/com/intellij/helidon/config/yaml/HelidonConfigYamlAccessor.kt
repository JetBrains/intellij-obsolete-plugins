// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.MetaConfigKeyManager.ConfigKeyNameBinder
import com.intellij.microservices.jvm.config.MicroservicesConfigBundle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.util.IncorrectOperationException
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.*
import java.util.*

internal class HelidonConfigYamlAccessor private constructor(private val root: YAMLPsiElement,
                                                             private val module: Module?) {

  internal constructor(root: YAMLDocument, module: Module? = null) : this(root as YAMLPsiElement, module)

  internal constructor(root: YAMLKeyValue, module: Module? = null) : this(root as YAMLPsiElement, module)

  constructor(root: YAMLSequenceItem, module: Module? = null) : this(root as YAMLPsiElement, module)

  internal fun findExistingKey(qualifiedKey: String): YAMLKeyValue? {
    if (qualifiedKey.isBlank()) return null

    val keyParts = splitQualifiedKey(qualifiedKey)
    val binder = getBinder()

    var searchElement = getTopLevelValue()
    for (i in keyParts.indices) {
      if (searchElement !is YAMLMapping) {
        return null
      }
      val subKey = keyParts[i]
      val relaxedChild = findChildRelaxed(searchElement, subKey, binder)
      if (relaxedChild == null || i + 1 == keyParts.size) {
        return relaxedChild
      }
      searchElement = relaxedChild.value
    }
    throw IllegalStateException("Should have returned from the loop '$qualifiedKey'")
  }

  private fun splitQualifiedKey(qualifiedKey: String): List<String> {
    val result = ArrayList<String>()
    val tokenizer = StringTokenizer(qualifiedKey, ".\"", true)
    var part = ""
    var inQuotes = false
    while (tokenizer.hasMoreTokens()) {
      val token = tokenizer.nextToken()
      if (token == "\"") {
        if (inQuotes) {
          result.add(part)
          inQuotes = false
        }
        else {
          part = ""
          inQuotes = true
        }
      }
      else if (inQuotes) {
        part += token
      }
      else if (token != ".") {
        result.add(token)
      }
    }
    return result
  }

  private fun findChildRelaxed(searchElement: YAMLMapping, subKey: String, binder: ConfigKeyNameBinder): YAMLKeyValue? {
    val byKey = searchElement.getKeyValueByKey(subKey)
    if (byKey != null) return byKey

    for (value in searchElement.keyValues) {
      val name = value.name
      if (name != null && binder.matchesPart(subKey, name)) return value
    }
    return null
  }

  private fun getBinder(): ConfigKeyNameBinder {
    val module = this.module ?: ModuleUtilCore.findModuleForPsiElement(root) ?: error("could not find module for accessor document")
    return HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module)
  }

  private fun getTopLevelValue(): YAMLValue? {
    return when (root) {
      is YAMLDocument -> root.topLevelValue
      is YAMLKeyValue -> root.value
      is YAMLSequenceItem -> root.value
      else -> null
    }
  }

  @Throws(IncorrectOperationException::class)
  fun create(qualifiedKey: String): YAMLKeyValue? {
    val keyParts = splitQualifiedKey(qualifiedKey)
    val topLevelValue = getTopLevelValue()
    val writeRunnable: Runnable
    if (topLevelValue !is YAMLMapping) {
      if (root is YAMLKeyValue) {
        val indent = YAMLUtil.getIndentToThisElement(root) + 2
        val dummyKeyValue = createKeyValue(keyParts, indent)
        writeRunnable = Runnable { root.setValue(dummyKeyValue.parentMapping!!) }
      }
      else {
        val generator = YAMLElementGenerator.getInstance(root.project)
        val dummyFile = generator.createDummyYamlWithText(YAMLElementGenerator.createChainedKey(keyParts, 0))
        val dummyValue = dummyFile.documents[0].topLevelValue!!
        writeRunnable = Runnable { topLevelValue?.replace(dummyValue) ?: root.add(dummyValue) }
      }
    }
    else {
      var topMostExistingMapping: YAMLMapping? = topLevelValue
      var topMostExistingKey: YAMLKeyValue? = null
      var foundHierarchies = 0
      for (subKey in keyParts) {
        if (topMostExistingMapping == null) {
          break
        }
        topMostExistingKey = findChildRelaxed(topMostExistingMapping, subKey, getBinder())
        if (topMostExistingKey == null) {
          break
        }
        topMostExistingMapping = topMostExistingKey.value as? YAMLMapping
        foundHierarchies++
      }
      if (foundHierarchies == keyParts.size) {
        throw IncorrectOperationException("key exists already: $qualifiedKey\n${root.text}")
      }
      assert(topMostExistingKey != null || topMostExistingMapping != null)
      val indent: Int = if (topMostExistingMapping != null) {
        YAMLUtil.getIndentToThisElement(topMostExistingMapping)
      }
      else {
        YAMLUtil.getIndentToThisElement(topMostExistingKey!!) + 2
      }
      val dummyKeyValue = createKeyValue(keyParts.subList(foundHierarchies, keyParts.size), indent)
      writeRunnable = if (topMostExistingMapping == null) {
        val finalTopMostExistingKey = topMostExistingKey
        Runnable { finalTopMostExistingKey!!.setValue(dummyKeyValue.parentMapping!!) }
      }
      else {
        val finalTopMostExistingMapping: YAMLMapping = topMostExistingMapping
        Runnable { finalTopMostExistingMapping.putKeyValue(dummyKeyValue) }
      }
    }
    WriteCommandAction.runWriteCommandAction(root.project,
                                             MicroservicesConfigBundle.message("config.insert.key", qualifiedKey),
                                             null,
                                             writeRunnable)
    return findExistingKey(qualifiedKey)
  }

  private fun createKeyValue(keyComponents: List<String>, indent: Int): YAMLKeyValue {
    val chainedKey = YAMLElementGenerator.createChainedKey(keyComponents, indent)
    val dummyFile = YAMLElementGenerator.getInstance(root.project).createDummyYamlWithText(chainedKey)
    val topLevelKeys = YAMLUtil.getTopLevelKeys(dummyFile)
    check(!topLevelKeys.isEmpty()) { "no top level keys (" + chainedKey + "): " + root.text }
    val dummyKeyValue = topLevelKeys.iterator().next()
    checkNotNull(dummyKeyValue.parentMapping) { "no containing mapping for a kv (" + chainedKey + "): " + root.text }
    return dummyKeyValue
  }
}