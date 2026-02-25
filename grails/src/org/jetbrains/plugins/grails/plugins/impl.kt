/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.plugins

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlFile
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

internal const val pluginClassSuffix = "GrailsPlugin"

fun GrailsApplication.computePlugins(): Collection<GrailsPluginDescriptor> = runReadAction {
  CachedValuesManager.getManager(project).getCachedValue(this) {
    Result.create(getSourcePlugins() + doComputeCompiledPlugins(), ProjectRootManager.getInstance(project))
  }
}

fun GrailsApplication.getSourcePlugins(): Collection<Grails3SourcePluginDescriptor> = runReadAction {
  CachedValuesManager.getManager(project).getCachedValue(this) {
    Result.create(doComputeSourcePlugins(), ProjectRootManager.getInstance(project))
  }
}

private fun GrailsApplication.doComputeSourcePlugins(): Collection<Grails3SourcePluginDescriptor> {
  return AllClassesSearch.search(getScope(true, false), project) { className ->
    className.endsWith(pluginClassSuffix)
  }.asIterable().mapNotNull {
    val application = GrailsApplicationManager.findApplication(it)
    if (application == null || application == this) null
    else Grails3SourcePluginDescriptor(it, application)
  }
}

private fun GrailsApplication.doComputeCompiledPlugins(): Collection<Grails3CompiledPluginDescriptor> {
  val scope = getScope(true, false)
  val facade = JavaPsiFacade.getInstance(project)
  val directories = facade.findPackage("META-INF")?.getDirectories(scope) ?: return emptyList()

  return directories.mapNotNull(fun(directory: PsiDirectory): Grails3CompiledPluginDescriptor? {
    val pluginXml = directory.findFile("grails-plugin.xml") as? XmlFile ?: return null
    val pluginClassFqn = pluginXml.rootTag?.findSubTags("type")?.firstOrNull()?.value?.trimmedText ?: return null
    val pluginClass = facade.findClass(pluginClassFqn, scope) ?: return null
    return Grails3CompiledPluginDescriptor(pluginClass) {
      pluginXml.rootTag?.getAttributeValue("version")
    }
  })
}
