/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.patterns.PsiMethodPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.ProcessingContext
import com.intellij.util.Processor
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyElementPattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyExpressionPattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames

class GsonRenderNamedArgumentsProvider : GroovyNamedArgumentProvider() {

  override fun getNamedArguments(call: GrCall,
                                 resolveResult: GroovyResolveResult,
                                 argumentName: String?,
                                 forCompletion: Boolean,
                                 result: MutableMap<String, NamedArgumentDescriptor>) {
    if (RENDER_METHOD_PATTERN_1.accepts(resolveResult.element)) {
      result["template"] = NamedArgumentDescriptor.TYPE_STRING
      result["collection"] = NamedArgumentDescriptor.SIMPLE_ON_TOP
      result["model"] = NamedArgumentDescriptor.TYPE_MAP
      result["var"] = NamedArgumentDescriptor.TYPE_STRING
//    result["bean"] = NamedArgumentDescriptor.SIMPLE_NORMAL
    }
  }

  override fun getNamedArguments(literal: GrListOrMap): Map<String, NamedArgumentDescriptor> {
    if (RENDER_MAP_PARAMETER.accepts(literal)) {
      return mapOf(
          "includes" to NamedArgumentDescriptor.TYPE_LIST,
          "excludes" to NamedArgumentDescriptor.TYPE_LIST,
          "deep" to NamedArgumentDescriptor.TYPE_BOOL
      )
    }
    return emptyMap()
  }
}

class GsonTemplateReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    val parent = element.parent
    if (parent !is GrNamedArgument) return emptyArray()
    if (parent.expression != element) return emptyArray()
    return arrayOf(GsonTemplateReference(element as GrLiteral))
  }
}

class GsonTemplateReference(literal: GrLiteral) : PsiReferenceBase<GrLiteral>(literal) {

  override fun resolve(): PsiElement? {
    val value = element.value as? String ?: return null
    val virtualFile = element.containingFile?.virtualFile ?: return null
    val template = if (value.startsWith("/")) {
      val templateName = value.split('/').lastOrNull()
      val templatePath = value.replaceAfterLast('/', "_$templateName${GsonConstants.FILE_SUFFIX}")
      val viewsRoot = ProjectFileIndex.getInstance(element.project).getSourceRootForFile(virtualFile) ?: return null
      VfsUtilCore.findRelativeFile(templatePath, viewsRoot)
    }
    else {
      virtualFile.parent?.findChild("_$value${GsonConstants.FILE_SUFFIX}")
    }
    return PsiManager.getInstance(element.project).findFile(template ?: return null)
  }

  override fun handleElementRename(newElementName: String): PsiElement? {
    val current = element.value as? String ?: throw IllegalStateException()
    val templateName = getGsonTemplateName(newElementName)
    val newContent = if (current.startsWith("/")) {
      current.replaceAfterLast('/', templateName)
    }
    else {
      templateName
    }
    return super.handleElementRename(newContent)
  }
}

class GsonTemplateReferenceQueryExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

  override fun processQuery(p: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
    val element = p.elementToSearch as? GroovyFile ?: return
    val templateName = getGsonTemplateName(element) ?: return
    p.optimizer.searchWord(templateName, p.effectiveSearchScope, UsageSearchContext.IN_STRINGS, true, element)

    val virtualFile = element.virtualFile
    val viewsRoot = ProjectFileIndex.getInstance(element.project).getSourceRootForFile(virtualFile) ?: return
    val path = VfsUtilCore.getRelativePath(virtualFile.parent, viewsRoot) ?: return
    val templateFqn = "/$path/$templateName"
    p.optimizer.searchWord(templateFqn, p.effectiveSearchScope, UsageSearchContext.IN_STRINGS, true, element)
  }
}

private val RENDER_METHOD_BASE_PATTERN = PsiMethodPattern().withName("render").definedInClass(
    "grails.plugin.json.view.api.GrailsJsonViewHelper"
)

// GrailsJsonViewHelper#render(java.util.Map)
private val RENDER_METHOD_PATTERN_1 = RENDER_METHOD_BASE_PATTERN.withParameters(
    CommonClassNames.JAVA_UTIL_MAP
)

// GrailsJsonViewHelper#render(java.lang.Object, java.util.Map)
private val RENDER_METHOD_PATTERN_2 = RENDER_METHOD_BASE_PATTERN.withParameters(
    CommonClassNames.JAVA_LANG_OBJECT, CommonClassNames.JAVA_UTIL_MAP
)

// GrailsJsonViewHelper#render(java.lang.Object, java.util.Map, groovy.lang.Closure)
private val RENDER_METHOD_PATTERN_3 = RENDER_METHOD_BASE_PATTERN.withParameters(
    CommonClassNames.JAVA_LANG_OBJECT, CommonClassNames.JAVA_UTIL_MAP, GroovyCommonClassNames.GROOVY_LANG_CLOSURE
)

// second (java.util.Map) parameter of
// GrailsJsonViewHelper#render(java.lang.Object, java.util.Map)
// GrailsJsonViewHelper#render(java.lang.Object, java.util.Map, groovy.lang.Closure)
private val RENDER_MAP_PARAMETER = GroovyExpressionPattern.Capture(GrListOrMap::class.java).methodCallParameter(
    1, StandardPatterns.or(RENDER_METHOD_PATTERN_2, RENDER_METHOD_PATTERN_3)
)

// g.render(template: "<place>")
val TEMPLATE_REFERENCE_PLACE: GroovyElementPattern.Capture<GrLiteralImpl> = GroovyPatterns.stringLiteral().withParent(
    GroovyPatterns.namedArgument().withLabel("template").withParent(
        GroovyPatterns.groovyElement().withParent(
            GroovyPatterns.methodCall().withMethod(RENDER_METHOD_PATTERN_1)
        )
    )
)