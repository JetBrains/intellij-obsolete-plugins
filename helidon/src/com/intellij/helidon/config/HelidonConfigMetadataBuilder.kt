// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.ConfigKeyDocumentationProviderBase
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey.Deprecation
import com.intellij.microservices.jvm.config.MetaConfigKey.DescriptionText
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.Processor
import com.intellij.util.Processors
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.FactoryMap
import com.siyeh.ig.psiutils.TypeUtils

private val DUMMY_TYPE: Pair<PsiType, MetaConfigKey.AccessType> = Pair(PsiTypes.nullType(), MetaConfigKey.AccessType.NORMAL)

private val METHOD_FQN_REGEX: Regex = Regex("([\\w.]+)#(\\w+).*")

private val EVERYTHING_PROCESSOR: Processor<MetaConfigKey> = Processor { true }

/**
 * Builds a tree of config properties based on the given list of [com.intellij.helidon.config.ModuleMetadata].
 *
 * Have to process all metadata because config types from different modules related to each other.
 *
 * @see HelidonConfigMetadataParser
 */
internal class HelidonConfigMetadataBuilder(modulesMetadata: List<ModuleMetadata>,
                                            private val project: Project) {

  private val myClassCaches: Map<GlobalSearchScope, ClassCache> = modulesMetadata.map { it.resolveScope }
    .associateWith { searchScope -> ClassCache(project, searchScope) }

  private val myConfigTypes: Map<String, ConfigType> = modulesMetadata.flatMap { it.moduleConfigs }
    .flatMap { moduleConfig -> moduleConfig.types }
    .associateBy { configType -> configType.type }

  internal fun collectKeys(module: Module): List<MetaConfigKey> {
    val metaKeys = mutableListOf<MetaConfigKey>()
    processMetadata(Processors.cancelableCollectProcessor(metaKeys), module)
    return metaKeys
  }

  private fun processMetadata(processor: Processor<MetaConfigKey>, module: Module) {
    myConfigTypes.values
      .filter { it.standalone && it.prefix.isNotBlank() }
      .forEach { processConfigType(it, it.prefix, processor, module, mutableSetOf()) }
  }

  private fun processConfigType(configType: ConfigType,
                                prefix: String,
                                processor: Processor<MetaConfigKey>,
                                module: Module,
                                processedConfigTypes: MutableSet<ConfigType>): List<HelidonMetaConfigKey> {
    if (!processedConfigTypes.add(configType)) return emptyList()

    val keys = mutableListOf<HelidonMetaConfigKey>()
    for (configOption in configType.options) {
      keys += processConfigOption(configOption, prefix, configType, processor, module, processedConfigTypes)
    }
    getInheritedConfigTypes(configType).forEach {
      keys += processConfigType(it, prefix, processor, module, processedConfigTypes)
    }
    return keys
  }

  private fun getInheritedConfigTypes(configType: ConfigType): List<ConfigType> = configType.inherits.mapNotNull { myConfigTypes[it] }

  private fun processConfigOption(configOption: ConfigOption,
                                  prefix: String,
                                  configType: ConfigType,
                                  processor: Processor<MetaConfigKey>,
                                  module: Module,
                                  processedConfigTypes: MutableSet<ConfigType>): List<HelidonMetaConfigKey> {
    val (psiType, accessType) = parsePsiTypeAndAccessType(configOption, project) ?: return emptyList()

    val keys = mutableListOf<HelidonMetaConfigKey>()

    if (isLeafConfigOption(psiType, configOption, configType.resolveScope)) {
      keys += processPrimitiveConfigOption(configOption, prefix, psiType, accessType, configType, processor, module, processedConfigTypes)
    }
    else {
      myConfigTypes[configOption.type]?.let { nestedConfigType ->
        keys += processConfigType(nestedConfigType, concatPrefixAndKey(prefix, configOption.key), processor, module, processedConfigTypes)
      }
    }
    return keys
  }

  private fun concatPrefixAndKey(prefix: String, key: String): String = if (prefix.isNotBlank()) "$prefix.$key" else key

  private fun processPrimitiveConfigOption(configOption: ConfigOption,
                                           prefix: String,
                                           configOptionType: PsiType,
                                           accessType: MetaConfigKey.AccessType,
                                           configType: ConfigType,
                                           processor: Processor<MetaConfigKey>,
                                           module: Module,
                                           processedConfigTypes: MutableSet<ConfigType>): List<HelidonMetaConfigKey> {
    val configOptionKey = concatPrefixAndKey(prefix, configOption.key)

    val (optionDeclaration, resolveResult) = parseDeclaration(configOption.method,
                                                              configOptionKey,
                                                              configOptionType,
                                                              configType) ?: return emptyList()

    val keys = mutableListOf<HelidonMetaConfigKey>()

    optionDeclaration.putUserData(ConfigKeyDocumentationProviderBase.CONFIG_KEY_DECLARATION_MODULE, module)

    val subKeys: List<HelidonMetaConfigKey> = if (accessType == MetaConfigKey.AccessType.INDEXED) {
      myConfigTypes[configOption.type]?.let {
        processConfigType(it, "", EVERYTHING_PROCESSOR, module, processedConfigTypes)
      } ?: emptyList()
    }
    else {
      emptyList()
    }

    val metaKey = HelidonMetaConfigKey(
      configOptionKey,
      optionDeclaration,
      resolveResult,
      configOptionType,
      getOptionDescription(configOption),
      getOptionDeprecation(configOption),
      configOption.defaultValue,
      accessType,
      subKeys)

    keys += metaKey

    processor.process(metaKey)

    return keys
  }

  private fun getOptionDescription(configOption: ConfigOption): DescriptionText {
    return if (configOption.description.isNotEmpty())
      DescriptionText(configOption.description)
    else
      DescriptionText.NONE
  }

  private fun getOptionDeprecation(configOption: ConfigOption): Deprecation {
    return if (configOption.deprecated)
      Deprecation.DEPRECATED_WITHOUT_REASON
    else
      Deprecation.NOT_DEPRECATED
  }

  private fun isLeafConfigOption(psiType: PsiType,
                                 configOption: ConfigOption,
                                 resolveScope: GlobalSearchScope): Boolean {
    if (configOption.kind != ConfigOptionKind.VALUE) return true

    return TypeConversionUtil.isPrimitiveWrapper(psiType) ||
           TypeUtils.isJavaLangString(psiType) ||
           CommonClassNames.JAVA_LANG_CLASS == configOption.type ||
           isEnum(configOption, resolveScope)
  }

  private fun isEnum(configOption: ConfigOption, resolveScope: GlobalSearchScope): Boolean {
    return myClassCaches[resolveScope]?.get(configOption.type)?.isEnum ?: false
  }

  private fun parseDeclaration(optionMethod: String?,
                               optionKey: String,
                               optionType: PsiType,
                               configType: ConfigType): Pair<PsiElement, MetaConfigKey.DeclarationResolveResult>? {
    val (classFqn, methodName) = parseClassAndMethod(optionMethod) ?: return null

    val classCache = myClassCaches[configType.resolveScope] ?: return null

    val sourceTypeClass = classCache.get(classFqn) ?: return null

    return Pair(HelidonConfigKeyDeclarationPsiElement(optionKey,
                                                      classFqn,
                                                      findPropertyNavigationTarget(sourceTypeClass, methodName),
                                                      sourceTypeClass,
                                                      configType.moduleName,
                                                      optionType),
                MetaConfigKey.DeclarationResolveResult.PROPERTY)
  }

  private fun parseClassAndMethod(methodFqn: String?): Pair<String, String>? {
    if (methodFqn == null) return null

    val matchResult = METHOD_FQN_REGEX.find(methodFqn) ?: return null
    val groups = matchResult.groupValues

    val classFqn = groups.getOrNull(1) ?: return null
    val method = groups.getOrNull(2) ?: return null

    return Pair(classFqn, method)
  }

  private fun findPropertyNavigationTarget(sourceTypeClass: PsiClass, methodName: String): PsiElement {
    return sourceTypeClass.findMethodsByName(methodName, true).firstOrNull()
           ?: sourceTypeClass
  }

  private fun parsePsiTypeAndAccessType(configOption: ConfigOption, project: Project): Pair<PsiType, MetaConfigKey.AccessType>? {
    val optionType = getActualConfigOptionType(configOption)

    val myCachedTypes = CachedValuesManager.getManager(project).getCachedValue(project) {
      val myElementFactory = JavaPsiFacade.getInstance(project).elementFactory

      val myFactoryMap = ConcurrentFactoryMap.createMap { key: String? ->
        if (key == null) return@createMap DUMMY_TYPE

        try {
          val psiType = myElementFactory.createTypeFromText(key.replace('$', '.'), null)
          return@createMap Pair(psiType, MetaConfigKey.AccessType.forPsiType(psiType))
        }
        catch (e: IncorrectOperationException) {
          return@createMap DUMMY_TYPE
        }
      }
      CachedValueProvider.Result.create(myFactoryMap, PsiModificationTracker.MODIFICATION_COUNT)
    }
    return myCachedTypes[optionType]
  }

  private fun getActualConfigOptionType(configOption: ConfigOption): String {
    return if (configOption.kind == ConfigOptionKind.LIST)
      "${CommonClassNames.JAVA_UTIL_LIST}<${configOption.type}>"
    else
      configOption.type
  }

  private class ClassCache(private val project: Project,
                           private val searchScope: GlobalSearchScope) {

    private val classes: MutableMap<String, PsiClass> = FactoryMap.create { fqn ->
      JavaPsiFacade.getInstance(project).findClass(fqn, searchScope)
    }

    fun get(fqn: String): PsiClass? = classes[fqn]
  }
}