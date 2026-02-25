/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.spring

import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiJavaPatterns.psiClass
import com.intellij.semantic.SemContributor
import com.intellij.semantic.SemRegistrar
import com.intellij.spring.model.jam.stereotype.SpringConfiguration

class GrailsSpringSemContributor : SemContributor() {
  protected override fun isAvailable(project: Project): Boolean {
    return JavaLibraryUtil.hasLibraryClass(project, "grails.boot.config.GrailsAutoConfiguration")
  }

  override fun registerSemProviders(registrar: SemRegistrar, project: Project) {
    registrar.registerSemElementProvider(
      SpringConfiguration.META_KEY,
      psiClass().inheritorOf(true, "grails.boot.config.GrailsAutoConfiguration")
    ) { SpringConfiguration.META }
  }
}