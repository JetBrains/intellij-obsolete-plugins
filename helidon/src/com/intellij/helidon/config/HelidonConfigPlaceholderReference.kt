// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.helidon.HelidonIcons
import com.intellij.lang.properties.psi.PropertiesElementFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.PropertyKeyIndex
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.ArrayUtil
import com.intellij.util.PairProcessor
import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull

class HelidonConfigPlaceholderReference private constructor(builder: Builder) :
  PsiReferenceBase.Poly<PsiElement>(builder.element, builder.range, builder.soft), HighlightedReference, ConfigPlaceholderReference {

  data class Builder(val element: PsiElement?, val range: TextRange?, val soft: Boolean) {

    var withSystemProperties: Boolean = false
      private set

    fun withSystemProperties(): Builder {
      withSystemProperties = true
      return this
    }

    fun build(): HelidonConfigPlaceholderReference = HelidonConfigPlaceholderReference(this)
  }

  private val withSystemProperties: Boolean = builder.withSystemProperties

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult?> {
    val key = value
    if (withSystemProperties) {
      val systemProperty = getSystemProperties().findPropertyByKey(key)
      if (systemProperty != null) {
        return PsiElementResolveResult.createResults(systemProperty.psiElement)
      }
    }

    val module = ModuleUtilCore.findModuleForPsiElement(element)

    val existingKeys = SmartList<PsiElement>()
    processKeys(module, PairProcessor { contributor: HelidonConfigFileContributor, psiFile: PsiFile ->
      existingKeys.addIfNotNull(contributor.findKey(psiFile, key))
      return@PairProcessor true
    })
    if (existingKeys.isNotEmpty()) {
      return PsiElementResolveResult.createResults(existingKeys)
    }

    // fallback to key in any .properties file
    val contentScope = module?.moduleContentScope ?: return ResolveResult.EMPTY_ARRAY
    val properties = PropertyKeyIndex.getInstance().getProperties(key, element.project, element.resolveScope.uniteWith(contentScope))
    return if (properties.isEmpty()) ResolveResult.EMPTY_ARRAY else PsiElementResolveResult.createResults(properties)
  }

  private fun getSystemProperties(): PropertiesFile {
    return PropertiesElementFactory.getSystemProperties(myElement.project)
  }

  override fun getVariants(): Array<Any> {
    val variants: MutableList<LookupElement> = ArrayList()

    val module = ModuleUtilCore.findModuleForPsiElement(element)
    val existingKeys: MutableSet<String> = HashSet()
    processKeys(module, PairProcessor { contributor: HelidonConfigFileContributor, psiFile: PsiFile ->
      val existingKeyVariants = contributor.getKeyVariants(psiFile)
      variants.addAll(existingKeyVariants)
      for (variant in existingKeyVariants) {
        existingKeys.add(variant.lookupString)
      }
      return@PairProcessor true
    })
    if (withSystemProperties) {
      for (property in getSystemProperties().properties) {
        val key = property.key ?: continue
        variants.add(LookupElementBuilder.create(property, key).withIcon(HelidonIcons.Helidon))
      }
    }
    return ArrayUtil.toObjectArray(variants)
  }

  private fun processKeys(module: Module?, processor: PairProcessor<HelidonConfigFileContributor, PsiFile>) {
    if (module == null) return

    val containingFile = element.containingFile.originalFile.virtualFile
    val isInTests = containingFile != null &&
                    ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(containingFile)

    val psiManager = PsiManager.getInstance(module.project)
    for (contributor in HelidonConfigFileContributor.EP_NAME.extensionList) {
      for (virtualFile in contributor.findConfigFiles(module, isInTests)) {
        val psiFile = psiManager.findFile(virtualFile) ?: continue
        if (!processor.process(contributor, psiFile)) return
      }
    }
  }
}