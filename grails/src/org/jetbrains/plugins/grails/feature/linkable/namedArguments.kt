/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.feature.linkable

import com.intellij.openapi.util.Key
import com.intellij.patterns.PsiMethodPattern
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.infos.CandidateInfo
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor
import org.jetbrains.plugins.groovy.extensions.impl.StringTypeCondition
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall

internal const val LINK_FQN = "grails.rest.Link"

internal val LINK_METHOD_KEY = Key.create<Any>("grails.linkable.method.key")
internal val LINK_METHOD_MARKER = Object()

private val STRING_DESCRIPTOR = LinkDescriptor(NamedArgumentDescriptor.TYPE_STRING)
private val BOOL_DESCRIPTOR = LinkDescriptor(NamedArgumentDescriptor.TYPE_BOOL)
private val LINK_NAMED_ARGS = mapOf(
    "rel" to STRING_DESCRIPTOR,
    "href" to STRING_DESCRIPTOR,
    "hreflang" to LinkDescriptor(StringTypeCondition("java.util.Locale")),
    "contentType" to STRING_DESCRIPTOR,
    "title" to STRING_DESCRIPTOR,
    "deprecated" to BOOL_DESCRIPTOR,
    "templated" to BOOL_DESCRIPTOR
)

// grails.rest.Link#createLink(java.util.Map)
private val LINK_METHOD_PATTERN = PsiMethodPattern().withName("createLink").definedInClass(
    LINK_FQN
).withParameters(CommonClassNames.JAVA_UTIL_MAP)

private class LinkDescriptor(delegate: NamedArgumentDescriptor) : NamedArgumentDescriptor by delegate {

  override fun createReference(label: GrArgumentLabel) = LinkReference(label)
}

private class LinkReference(label: GrArgumentLabel) : PsiPolyVariantReferenceBase<GrArgumentLabel>(label) {

  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    val name = element.name ?: return emptyArray()
    val clazz = JavaPsiFacade.getInstance(element.project).findClass(LINK_FQN, element.resolveScope) ?: return emptyArray()
    return clazz.findFieldByName(name, false)?.let {
      arrayOf(CandidateInfo(it, PsiSubstitutor.EMPTY))
    } ?: emptyArray()
  }
}

class LinkableNamedArgumentsProvider : GroovyNamedArgumentProvider() {

  override fun getNamedArguments(call: GrCall,
                                 resolveResult: GroovyResolveResult,
                                 argumentName: String?,
                                 forCompletion: Boolean,
                                 result: MutableMap<String, NamedArgumentDescriptor>) {
    val resolved = resolveResult.element
    if (resolved?.getUserData(LINK_METHOD_KEY) == LINK_METHOD_MARKER || LINK_METHOD_PATTERN.accepts(resolved)) {
      result += LINK_NAMED_ARGS
    }
  }
}

