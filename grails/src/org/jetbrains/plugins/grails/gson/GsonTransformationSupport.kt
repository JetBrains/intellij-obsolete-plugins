/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.references.TraitInjectorService
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class GsonTransformationSupport : AstTransformationSupport {

  private val VIEW_TYPE = "views"
  private val GSON_VIEW_TYPE = "view.gson"

  override fun applyTransformation(context: TransformationContext) {
    if (context.codeClass !is GroovyScriptClass) return
    val scriptClass = context.codeClass as GroovyScriptClass
    val file = scriptClass.containingFile
    if (!file.name.endsWith(GsonConstants.FILE_SUFFIX)) return

    findModelClosure(file)?.acceptChildren(object : GroovyElementVisitor() {
      override fun visitVariableDeclaration(variableDeclaration: GrVariableDeclaration) {
        for (variable in variableDeclaration.variables) {
          context.addField(GrScriptField(variable, scriptClass))
        }
      }
    })
    context.setSuperType("grails.plugin.json.view.JsonViewTemplate")
    val s = { fqn: String -> context.addInterface(fqn) }
    TraitInjectorService.getInjectedTraits(context.codeClass, VIEW_TYPE).forEach(s)
    TraitInjectorService.getInjectedTraits(context.codeClass, GSON_VIEW_TYPE).forEach(s)
  }
}
