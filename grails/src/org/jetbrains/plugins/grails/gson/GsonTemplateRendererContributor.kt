/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods

class GsonTemplateRendererContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String = "grails.plugin.json.view.api.internal.TemplateRenderer"

  override fun processDynamicElements(qualifierType: PsiType, processor: PsiScopeProcessor, place: PsiElement, state: ResolveState) {
    if (!processor.shouldProcessMethods()) return
    val name = ResolveUtil.getNameHint(processor) ?: return

    GrLightMethodBuilder(place.manager, name).apply {
      addParameter("value", CommonClassNames.JAVA_LANG_OBJECT)
      setReturnType("grails.plugin.json.builder.JsonOutput.JsonUnescaped", place.resolveScope)
      if (!processor.execute(this, state)) return
    }

    GrLightMethodBuilder(place.manager, name).apply {
      addParameter("var", CommonClassNames.JAVA_LANG_OBJECT)
      addParameter("collection", CommonClassNames.JAVA_LANG_ITERABLE)
      setReturnType("grails.plugin.json.builder.JsonOutput.JsonUnescaped", place.resolveScope)
      if (!processor.execute(this, state)) return
    }
  }
}

