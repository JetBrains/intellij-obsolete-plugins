/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.tests

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.grails.structure.GrailsApplication

fun getTestsForArtifact(application: GrailsApplication, artefact: PsiClass, result: MutableCollection<in PsiClass>): Unit {
  val module = ModuleUtil.findModuleForPsiElement(artefact) ?: return

  val qualifiedName = artefact.qualifiedName ?: return
  val packageName = StringUtil.getPackageName(qualifiedName)
  val shortName = StringUtil.getShortName(qualifiedName)
  val unitTestFqn = StringUtil.getQualifiedName(packageName, shortName + "Spec")

  val scope = GlobalSearchScope.moduleWithDependentsScope(module)
  val clazz = JavaPsiFacade.getInstance(application.project).findClass(unitTestFqn, scope) ?: return
  result.add(clazz)
}