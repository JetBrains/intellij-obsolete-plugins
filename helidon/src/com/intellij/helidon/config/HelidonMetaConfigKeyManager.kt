// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.utils.HelidonCommonUtils
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.microservices.jvm.config.utils.findConfigFilesInMetaInf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

@Service(Service.Level.APP)
class HelidonMetaConfigKeyManager : MetaConfigKeyManager() {
  companion object {
    fun getInstance(): HelidonMetaConfigKeyManager = ApplicationManager.getApplication().service()
  }

  override fun getAllMetaConfigKeys(module: Module?): List<MetaConfigKey> {
    if (module == null || !HelidonCommonUtils.hasHelidonLibrary(module)) return emptyList()

    return getMetaConfigKeysFromLibs(module)
  }

  override fun getConfigKeyNameBinder(module: Module): ConfigKeyNameBinder = HelidonConfigKeyNameBinder

  private fun getMetaConfigKeysFromLibs(module: Module): List<MetaConfigKey> {
    return CachedValuesManager.getManager(module.project).getCachedValue(module) {
      val modulesMetadata = findConfigFilesInMetaInf<PsiFile>(module, HELIDON_CONFIG_METADATA, true).mapNotNull { metadataFile ->
        getModuleMetadataForFile(metadataFile)
      }
      val allKeys = HelidonConfigMetadataBuilder(modulesMetadata, module.project).collectKeys(module)
      CachedValueProvider.Result.create(allKeys, PsiModificationTracker.MODIFICATION_COUNT)
    }
  }

  private fun getModuleMetadataForFile(configMetadataFile: PsiFile): ModuleMetadata? {
    try {
      return HelidonConfigMetadataParser().parse(configMetadataFile)
    }
    catch (ce: ProcessCanceledException) {
      throw ce
    }
    catch (e: Exception) {
      logger<HelidonMetaConfigKeyManager>().warn("Error parsing " + configMetadataFile.virtualFile.path, e)
    }
    return null
  }
}