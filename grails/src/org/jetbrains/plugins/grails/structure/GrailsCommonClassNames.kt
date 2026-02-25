/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.grails.util.version.Version

class GrailsCommonClassNames private constructor(globalPrefix: String, constraintPath: String) {

  companion object {
    private val after40: GrailsCommonClassNames = GrailsCommonClassNames("org.grails.", "datastore.gorm.validation.constraints.")
    private val after30: GrailsCommonClassNames = GrailsCommonClassNames("org.grails.", "validation.")
    private val before30: GrailsCommonClassNames = GrailsCommonClassNames("org.codehaus.groovy.grails.", "validation.")

    @JvmStatic
    fun getInstance(context: PsiElement): GrailsCommonClassNames {
      val app: GrailsApplication = GrailsApplicationManager.findApplication(context) ?: return before30
      if (Version.AT_LEAST_4.contains(app.grailsVersion)) {
        return after40
      }
      else if (Version.AT_LEAST_3.contains(app.grailsVersion)) {
        return after30
      }
      else {
        return before30
      }
    }

    private const val HIBERNATE_MAPPING_BUILDER = "orm.hibernate.cfg.HibernateMappingBuilder"
    private const val CREDIT_CARD_CONSTRAINT    = "CreditCardConstraint"
    private const val MAX_SIZE_CONSTRAINT       = "MaxSizeConstraint"
    private const val EMAIL_CONSTRAINT          = "EmailConstraint"
    private const val BLANK_CONSTRAINT          = "BlankConstraint"
    private const val RANGE_CONSTRAINT          = "RangeConstraint"
    private const val URL_CONSTRAINT            = "UrlConstraint"
    private const val SIZE_CONSTRAINT           = "SizeConstraint"
    private const val IN_LIST_CONSTRAINT        = "InListConstraint"
    private const val MATCHES_CONSTRAINT        = "MatchesConstraint"
    private const val MIN_CONSTRAINT            = "MinConstraint"
    private const val MAX_CONSTRAINT            = "MaxConstraint"
    private const val MIN_SIZE_CONSTRAINT       = "MinSizeConstraint"
    private const val SCALE_CONSTRAINT          = "ScaleConstraint"
    private const val NOT_EQUAL_CONSTRAINT      = "NotEqualConstraint"
    private const val NULLABLE_CONSTRAINT       = "NullableConstraint"
    private const val VALIDATOR_CONSTRAINT      = "ValidatorConstraint"
    private const val UNIQUE_CONSTRAINT         = "UniqueConstraint"
    private const val URL_MAPPING_BUILDER       = "web.mapping.DefaultUrlMappingEvaluator.UrlMappingBuilder"
  }

  val hibernateMappingBuilder: String = globalPrefix + HIBERNATE_MAPPING_BUILDER
  val creditCardConstraint: String    = globalPrefix + constraintPath + CREDIT_CARD_CONSTRAINT
  val emailConstraint: String         = globalPrefix + constraintPath + EMAIL_CONSTRAINT
  val blankConstraint: String         = globalPrefix + constraintPath + BLANK_CONSTRAINT
  val rangeConstraint: String         = globalPrefix + constraintPath + RANGE_CONSTRAINT
  val inListConstraint: String        = globalPrefix + constraintPath + IN_LIST_CONSTRAINT
  val urlConstraint: String           = globalPrefix + constraintPath + URL_CONSTRAINT
  val sizeConstraint: String          = globalPrefix + constraintPath + SIZE_CONSTRAINT
  val matchesConstraint: String       = globalPrefix + constraintPath + MATCHES_CONSTRAINT
  val minConstraint: String           = globalPrefix + constraintPath + MIN_CONSTRAINT
  val maxConstraint: String           = globalPrefix + constraintPath + MAX_CONSTRAINT
  val minSizeConstraint: String       = globalPrefix + constraintPath + MIN_SIZE_CONSTRAINT
  val maxSizeConstraint: String       = globalPrefix + constraintPath + MAX_SIZE_CONSTRAINT
  val scaleConstraint: String         = globalPrefix + constraintPath + SCALE_CONSTRAINT
  val notEqualConstraint: String      = globalPrefix + constraintPath + NOT_EQUAL_CONSTRAINT
  val nullableConstraint: String      = globalPrefix + constraintPath + NULLABLE_CONSTRAINT
  val validatorConstraint: String     = globalPrefix + constraintPath + VALIDATOR_CONSTRAINT
  val uniqueConstraint: String        = globalPrefix + constraintPath + UNIQUE_CONSTRAINT
  val urlMappingBuilder: String       = globalPrefix + URL_MAPPING_BUILDER

}