/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.plugins

import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.structure.GrailsApplication

interface GrailsPluginDescriptor {

  val pluginClass: PsiClass
  val pluginVersion: String?
  val pluginName: String
    get() = pluginClass.name?.removeSuffix(pluginClassSuffix)?.decapitalize() ?: ""

}

class Grails3SourcePluginDescriptor(
    override val pluginClass: PsiClass,
    val pluginApplication: GrailsApplication
) : GrailsPluginDescriptor {

  override val pluginVersion: String? get() = pluginApplication.appVersion
}

class Grails3CompiledPluginDescriptor(
    override val pluginClass: PsiClass,
    private val pluginVersionGetter: () -> String?
) : GrailsPluginDescriptor {

  override val pluginVersion: String? get() = pluginVersionGetter()

}
