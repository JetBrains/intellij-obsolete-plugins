// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.HelidonIcons
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import javax.swing.Icon

internal const val HELIDON_CONFIG_FQN = "io.helidon.config.Config"
internal const val HELIDON_CONFIG_GET_METHOD = "get"

internal const val HELIDON_APPLICATION_PREFIX = "application"
internal const val HELIDON_APPLICATION_ENV_SPECIFIC_PREFIX = "$HELIDON_APPLICATION_PREFIX-"
internal const val HELIDON_MP_CONFIG_FILE_NAME = "microprofile-config"

fun isHelidonConfigFileName(fileName: String): Boolean {
  return fileName == HELIDON_APPLICATION_PREFIX
         || fileName == HELIDON_MP_CONFIG_FILE_NAME
         || fileName.startsWith(HELIDON_APPLICATION_ENV_SPECIFIC_PREFIX)
}

fun isHelidonConfigFile(file: PsiFile): Boolean = getHelidonConfigFileIcon(file) != null

fun getHelidonConfigFileIcon(file: PsiFile): Icon? {
  if (file.virtualFile == null) return null

  val fileModule = ModuleUtilCore.findModuleForPsiElement(file)
  if (fileModule == null || !hasHelidonLibrary(fileModule)) return null

  return CachedValuesManager.getCachedValue(file) {
    val module = ModuleUtilCore.findModuleForPsiElement(file)
    if (module != null && !module.isDisposed) {
      val virtualFile = file.virtualFile
      val sourceRoots = ModuleRootManager.getInstance(module).sourceRoots
      for (contributor in HelidonConfigFileContributor.EP_NAME.extensions) {
        if (contributor.fileType == file.fileType) {
          if (isHelidonConfigFileName(virtualFile.nameWithoutExtension) && VfsUtilCore.isUnder(virtualFile, sourceRoots.toSet())) {
            return@getCachedValue Result.create<Icon?>(
              HelidonIcons.Helidon, file,
              JavaLibraryModificationTracker.getInstance(file.project))
          }
        }
      }
    }
    return@getCachedValue Result.create<Icon?>(null, file, JavaLibraryModificationTracker.getInstance(file.project))
  }
}

fun createHelidonPlaceholderReferences(element: PsiElement): Array<PsiReference> {
  return ConfigPlaceholderReference.createPlaceholderReferences(element) { psiElement, range ->
    HelidonConfigPlaceholderReference.Builder(psiElement, range, false)
      .withSystemProperties()
      .build()
  }
}