// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.config.isHelidonConfigFileName
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.jsonSchema.extension.JsonWidgetSuppressor
import org.jetbrains.yaml.psi.YAMLFile

internal class HelidonYamlJsonWidgetSuppressor : JsonWidgetSuppressor {
  override fun isCandidateForSuppress(file: VirtualFile, project: Project): Boolean {
    val psiFile = PsiManager.getInstance(project).findFile(file)
    return psiFile is YAMLFile && isHelidonConfigFileName(file.nameWithoutExtension)
  }

  override fun suppressSwitcherWidget(file: VirtualFile, project: Project): Boolean {
    if (!hasHelidonLibrary(project)) return false

    val psiFile = PsiManager.getInstance(project).findFile(file)
    return psiFile is YAMLFile && isHelidonConfigFile(psiFile)
  }
}