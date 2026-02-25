/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlFile
import com.intellij.util.containers.FactoryMap
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler
import org.jetbrains.plugins.grails.artefact.api.HandlerCache
import org.jetbrains.plugins.grails.artefact.api.allHandlers
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil

private const val GRAILS_ARTEFACT_ARTEFACT = "grails.artefact.Artefact"

fun GrailsArtefactHandler.getArtefacts(application: GrailsApplication, scope: GlobalSearchScope): Collection<PsiClass> {
  return CachedValuesManager.getManager(application.project).getCachedValue(application, keys[this]!!, {
    Result.create(createCache(application), PsiModificationTracker.MODIFICATION_COUNT)
  }, false)[scope] ?: emptyList()
}

private val keys: Map<GrailsArtefactHandler, Key<CachedValue<Map<GlobalSearchScope, Collection<PsiClass>>>>> = FactoryMap.create {
  Key.create("grails.artefact.cache." + it.javaClass.name)
}


private fun GrailsArtefactHandler.createCache(application: GrailsApplication): Map<GlobalSearchScope, Collection<PsiClass>> = FactoryMap.create {
  ApplicationManager.getApplication().runReadAction(Computable {
    this@createCache.doGetArtefacts(application, it)
  })
}

private fun GrailsArtefactHandler.doGetArtefacts(application: GrailsApplication, scope: GlobalSearchScope): Collection<PsiClass> {
  val result = linkedSetOf<PsiClass>()

  collectConventionalArtefacts(application, scope)?.let { result += it }
  collectCompiledArtefacts(application, scope)?.let { result += it }
  collectAnnotatedArtefacts(application, scope)?.let { result += it }
  collectSpecificAnnotatedArtefacts(application, scope).let { result += it }

  return result
}

/**
 * PsiClass is considered a convention artefact when all the following us true:
 * - its name has specified suffix
 * - its name if the same as the name of file
 * - it is defined in a groovy file which lies under specified directory
 */
private fun GrailsArtefactHandler.collectConventionalArtefacts(application: GrailsApplication,
                                                               scope: GlobalSearchScope): Collection<PsiClass>? {
  val project = application.project
  val directory = getDirectory(application) ?: return null
  val artefactDirectoryScope = GlobalSearchScopesCore.directoryScope(project, directory, true)
  val resultScope = artefactDirectoryScope.intersectWith(scope)
  return AllClassesSearch.search(resultScope, project) { className ->
    className.endsWith(artefactClassSuffix)
  }.asIterable().filter { artefactClass ->
    val file = artefactClass.containingFile?.virtualFile
    file?.nameWithoutExtension == artefactClass.name && file?.fileType == GroovyFileType.GROOVY_FILE_TYPE
  }
}

//private fun GrailsArtefactHandler.isConventionalArtefact(artefactClass: PsiClass, checkName: Boolean = false): Boolean {
//  val name = artefactClass.name ?: return false
//  val file = artefactClass.containingFile?.virtualFile
//  return file?.nameWithoutExtension == name && file?.fileType == GroovyFileType.GROOVY_FILE_TYPE
//}

/**
 * Collects classes defined in META-INF/grails-plugin.xml
 */
private fun GrailsArtefactHandler.collectCompiledArtefacts(application: GrailsApplication,
                                                           scope: GlobalSearchScope): Collection<PsiClass>? {
  val project = application.project
  val facade = JavaPsiFacade.getInstance(project)
  val metaInfPackage = facade.findPackage("META-INF") ?: return null
  val result = mutableListOf<PsiClass>()

  for (directory in metaInfPackage.getDirectories(scope)) {
    val file = directory.findFile("grails-plugin.xml") as? XmlFile ?: continue
    val resourcesTag = file.rootTag?.findSubTags("resources")?.firstOrNull() ?: continue
    for (tag in resourcesTag.subTags.filter { it.name == "resource" }) {
      val fqn = tag.value.trimmedText
      if (fqn.endsWith(artefactClassSuffix)) {
        facade.findClass(fqn, scope)?.let { result += it }
      }
    }
  }

  return result
}

/**
 * Collects classes annotated with @Artefact("artefactHandlerId"), where artefactId corresponds to the current artefact handler.
 */
private fun GrailsArtefactHandler.collectAnnotatedArtefacts(application: GrailsApplication,
                                                            scope: GlobalSearchScope): Collection<PsiClass>? {
  val project = application.project
  val annotationClassSearchScope = application.getScope(true, false)


  val annotationClass = JavaPsiFacade.getInstance(project).findClass(GRAILS_ARTEFACT_ARTEFACT,
                                                                     annotationClassSearchScope) ?: return null
  return AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).asIterable().filter { artefactClass ->
    artefactClass.containingFile?.virtualFile?.fileType == GroovyFileType.GROOVY_FILE_TYPE
    && artefactClass.name?.endsWith(artefactClassSuffix) ?: false
    && artefactClass.modifierList?.findAnnotation(GRAILS_ARTEFACT_ARTEFACT)?.let {
      GrAnnotationUtil.inferStringAttribute(it, "value") == artefactHandlerID
    } ?: false
  }
}

/**
 * Collects classes annotated with artefact specific annotation, i.e. @Controller or @Taglib.
 */
private fun GrailsArtefactHandler.collectSpecificAnnotatedArtefacts(application: GrailsApplication,
                                                                    scope: GlobalSearchScope) = annotationFqns.flatMap { annotationFqn ->
  val project = application.project
  val annotationClassSearchScope = application.getScope(true, false)
  JavaPsiFacade.getInstance(project).findClass(annotationFqn, annotationClassSearchScope)?.let { annotationClass ->
    AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).asIterable().filter { artefactClass ->
      artefactClass.containingFile?.virtualFile?.fileType == GroovyFileType.GROOVY_FILE_TYPE &&
      artefactClass.name?.endsWith(artefactClassSuffix) ?: false
    }
  } ?: emptyList()
}

fun getArtefactHandler(clazz: PsiClass): GrailsArtefactHandler? {
  if (clazz !is GrTypeDefinition) return null
  return CachedValuesManager.getCachedValue(clazz) {
    Result.create(doGetArtefactHandler(clazz), clazz)
  }
}

private fun doGetArtefactHandler(clazz: GrTypeDefinition): GrailsArtefactHandler? {
  val application = GrailsApplicationManager.findApplication(clazz) ?: return null
  val name = clazz.name ?: return null

  val handlerCache = service<HandlerCache>()
  // first check for @Artefact
  AnnotationUtil.findAnnotation(clazz, GRAILS_ARTEFACT_ARTEFACT)?.let { annotation ->
    GrAnnotationUtil.inferStringAttribute(annotation, "value")?.let { id ->
      handlerCache.idToHandler[id]?.let {
        return it
      }
    }
  }

  // check for @TagLib, @Entity, etc
  for ((fqn, handler) in handlerCache.annotationToHandler) {
    if (AnnotationUtil.findAnnotation(clazz, fqn) != null) {
      return handler
    }
  }

  val file = clazz.containingFile?.virtualFile ?: return null
  if (file.nameWithoutExtension != name) return null

  // check if it's conventional artefact
  for (handler in allHandlers) {
    if (!name.endsWith(handler.artefactClassSuffix)) continue

    val artefactDirectory = handler.getDirectory(application) ?: continue
    if (!VfsUtilCore.isAncestor(artefactDirectory, file, true)) continue

    return handler
  }

  return null
}
