/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.lang

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.removeUserData
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import org.jetbrains.plugins.grails.gorm.GormClassNames.ENTITY_TRAIT
import org.jetbrains.plugins.grails.gorm.GormVersion
import org.jetbrains.plugins.grails.gorm.getGormVersion
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyDirectInheritorsSearcher
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class GormTraitContributor : AstTransformationSupport {

  override fun applyTransformation(context: TransformationContext) {
    val clazz = context.codeClass
    if (GrailsUtils.calculateArtifactType(clazz) !== GrailsArtifact.DOMAIN) return

    val gormVersion = getGormVersion(clazz) ?: return
    if (gormVersion < GormVersion.IS_5) return
    if (gormVersion >= GormVersion.IS_6 && context.isInheritor("grails.gorm.rx.RxEntity")) return

    /*
    Repeat org.grails.compiler.gorm.GormEntityTransformation#pickGormEntityTrait().
     */
    val mapWith = computeMapWithValue(clazz)
    val hibernatePresent = JavaPsiFacade.getInstance(clazz.project).findClass("org.hibernate.Hibernate", clazz.resolveScope) != null
    val traitFqn = if (hibernatePresent && mapWith == null) {
      ENTITY_TRAIT
    }
    else {
      val providers = findTraitsFromProviders(clazz)
      if (providers.isEmpty()) {
        ENTITY_TRAIT
      }
      else if (mapWith == null || mapWith.isEmpty()) {
        providers.singleOrNull()?.qualifiedName ?: ENTITY_TRAIT
      }
      else {
        val traitFromProvider = providers.find { it.name?.startsWith(mapWith) ?: false }
        traitFromProvider?.qualifiedName ?: ENTITY_TRAIT
      }
    }
    Grails3TraitInjectorContributor.injectTraits(clazz, context, listOf(traitFqn))
  }

  private fun computeMapWithValue(clazz: GrTypeDefinition) = CachedValuesManager.getCachedValue(clazz) {
    Result.create(doComputeMapWithValue(clazz), PsiModificationTracker.MODIFICATION_COUNT)
  }

  private fun doComputeMapWithValue(clazz: GrTypeDefinition): String? {
    val mapWith = clazz.findCodeFieldByName("mapWith", true)
    if (mapWith is GrField) {
      val initializer = mapWith.initializerGroovy
      if (initializer is GrLiteral) {
        val value = initializer.value
        if (value is String) {
          return value.capitalize()
        }
      }
    }
    return null
  }

  private fun findTraitsFromProviders(context: PsiElement) = CachedValuesManager.getCachedValue(context) {
    Result.create(doFindTraitsFromProviders(context), ProjectRootManager.getInstance(context.project))
  }

  /**
   * Grails loads GormEntityTraitProvider using Java Services API and calls getEntityTrait().
   * To reduce processing time we assume that GormEntity's inheritors are registered in META-INF/services via GormEntityTraitProvider.
   * I.e. we do not run script with user classpath to determine traits, but just search for GormEntity inheritors.
   */
  private fun doFindTraitsFromProviders(context: PsiElement): Collection<PsiClass> {
    val scope = context.resolveScope
    val facade = JavaPsiFacade.getInstance(context.project)
    val gormEntity = facade.findClass(ENTITY_TRAIT, scope) ?: return emptyList()
    val version = getGormVersion(context) ?: return emptyList()
    val result = ObjectOpenCustomHashSet(object : Hash.Strategy<PsiClass> {
      override fun hashCode(`object`: PsiClass?) = `object`?.qualifiedName?.hashCode() ?: 0
      override fun equals(o1: PsiClass?, o2: PsiClass?) = o1 === o2 || o1?.qualifiedName == o2?.qualifiedName
    })
    gormEntity.putUserData(GroovyDirectInheritorsSearcher.IGNORE_INHERITANCE_CHECK, true)
    val query = ClassInheritorsSearch.search(gormEntity, scope, true)
    val traits = query.asIterable().asSequence().filter {
      GrTraitUtil.isTrait(it)
    }
    gormEntity.removeUserData(GroovyDirectInheritorsSearcher.IGNORE_INHERITANCE_CHECK)
    val availableTraits = if (version < GormVersion.IS_6) traits
    else traits.filter {
      val markerFqn = markerClasses[it.qualifiedName] ?: return@filter true
      facade.findClass(markerFqn, scope) != null
    }

    return availableTraits.toCollection(result)
  }

  private val markerClasses = mapOf(
      "grails.mongodb.MongoEntity" to "com.mongodb.MongoClient",
      "grails.neo4j.Neo4jEntity" to "org.neo4j.driver.v1.Driver"
  )
}