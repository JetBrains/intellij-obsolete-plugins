/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler as ArtefactHandler

internal fun getArtefactNodes(project: Project,
                              settings: ViewSettings,
                              artefactHandler: ArtefactHandler,
                              artefacts: Classes): TreeNodes {
  if (settings.isFlattenPackages) {
    val packages = getPackagesFlattened(artefacts, settings.isHideEmptyMiddlePackages)
    val packageNodes = packages.map { GrailsFlatPackageNode(project, settings, it) }
    val classNodes = artefacts.getClassNodes(project, settings, artefactHandler, emptyList())
    return packageNodes + classNodes
  }
  else {
    return getNodesRegular(artefacts, project, settings, artefactHandler, emptyList())
  }
}

private fun getPackagesFlattened(classes: Classes, hideEmptyMiddlePackages: Boolean): Collection<String> {
  val classPackageFqns = classes.mapNotNullTo(LinkedHashSet(), ::getPackageName)
  val packageFqns = classPackageFqns.toMutableSet()
  if (!hideEmptyMiddlePackages) {
    for (fqn in classPackageFqns) {
      var current = fqn
      while (current != "") {
        current = StringUtil.getPackageName(current)
        packageFqns += current
      }
    }
  }
  packageFqns.remove("")
  return packageFqns
}

internal fun Classes.getClassNodes(project: Project,
                                   settings: ViewSettings,
                                   artefactHandler: ArtefactHandler,
                                   packageFqn: List<String>): TreeNodes {
  return getClassNodes(project, settings, artefactHandler, packageFqn.fqnString())
}

internal fun Classes.getClassNodes(project: Project,
                                   settings: ViewSettings,
                                   artefactHandler: ArtefactHandler,
                                   packageFqn: String): TreeNodes {
  val classes = getClassesInPackage(packageFqn)
  return classes.map { artefactHandler.createNode(it, settings) ?: ClassTreeNode(project, it, settings) }
}

internal fun getNodesRegular(classes: Classes,
                             project: Project,
                             settings: ViewSettings,
                             artefactHandler: ArtefactHandler,
                             packageFqn: List<String>): TreeNodes {
  val packageFqns = getPackagesRegular(classes, packageFqn, settings.isHideEmptyMiddlePackages)
  val packageNodes = packageFqns.map { GrailsRegularPackageNode(project, settings, CompactedFqn(packageFqn, it)) }
  val classNodes = classes.getClassNodes(project, settings, artefactHandler, packageFqn)
  return packageNodes + classNodes
}

internal fun getPackagesRegular(classes: Classes, basePackage: List<String>, hideEmptyMiddlePackages: Boolean): Collection<List<String>> {
  val packagePrefix = basePackage.joinToString("") { "$it." }
  val fullPackageNames: Collection<String> = classes.mapNotNullTo(LinkedHashSet(), ::getPackageName).filter {
    it.startsWith(packagePrefix)
  }
  val relativePackageParts: Collection<List<String>> = fullPackageNames.asSequence().map {
    it.substringAfter(packagePrefix, "")
  }.map {
    it.split(".")
  }.filter {
    !it.isEmpty()
  }.toList()
  if (hideEmptyMiddlePackages) {
    val (nonEmptyPackageParts, emptyPackageParts) = relativePackageParts.partition { it.size == 1 }
    val nonEmptyPackageNames: Set<String> = nonEmptyPackageParts.mapTo(HashSet()) { it[0] }
    val otherPackageParts = emptyPackageParts.filter { it[0] !in nonEmptyPackageNames }
    return nonEmptyPackageParts + otherPackageParts
  }
  else {
    return relativePackageParts.mapTo(HashSet()) { it[0] }.map(::listOf)
  }
}

private fun getPackageName(clazz: PsiClass): String? = clazz.qualifiedName?.let(StringUtil::getPackageName)

private fun Classes.getClassesInPackage(basePackage: String) = filter { clazz ->
  getPackageName(clazz) == basePackage
}

fun List<String>.fqnString(): String = joinToString(".")
