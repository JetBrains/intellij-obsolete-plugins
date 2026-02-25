/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.lang

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class DirtyCheckableContributor : AstTransformationSupport {

  companion object {
    const val DIRTY_CHECK_FQN: String = "grails.gorm.dirty.checking.DirtyCheck"
    const val DIRTY_CHECKABLE_FQN: String = "org.grails.datastore.mapping.dirty.checking.DirtyCheckable"
    const val ORIGIN_INFO: String = "via @DirtyCheck"
  }

  /**
   * DirtyCheckable is added in compile time when:
   *
   * 1. class annotated explicitly -> org.codehaus.groovy.grails.compiler.gorm.DirtyCheckTransformation
   * 2. class is a GORM domain
   */
  private fun check(clazz: GrTypeDefinition) =
    AnnotationUtil.isAnnotated(clazz, DIRTY_CHECK_FQN, 0) || GrailsUtils.calculateArtifactType(clazz) === GrailsArtifact.DOMAIN

  /**
   * In GORM 5, DirtyCheckable was introduced as a trait.
   * If it is a trait, then we do not collect its methods here,
   * because they will be collected in
   * [TraitTransformationSupport][org.jetbrains.plugins.groovy.transformations.impl.TraitTransformationSupport].
   * If it is a simple interface, then we are dealing with pre GORM 5, where all its methods are added one by one to the target class.
   */
  override fun applyTransformation(context: TransformationContext) {
    val clazz = context.codeClass
    if (!check(clazz)) return
    context.addInterface(DIRTY_CHECKABLE_FQN)
    val dirtyCheckable = JavaPsiFacade.getInstance(clazz.project).findClass(DIRTY_CHECKABLE_FQN, clazz.resolveScope)
    if (dirtyCheckable == null || !dirtyCheckable.isInterface || GrTraitUtil.isTrait(dirtyCheckable)) return
    for (interfaceMethod in dirtyCheckable.methods) {
      val wrapper = GrMethodWrapper.wrap(interfaceMethod)
      wrapper.modifierList.removeModifier(GrModifierFlags.ABSTRACT_MASK)
      wrapper.originInfo = ORIGIN_INFO
      context.addMethod(wrapper)
    }
  }
}
