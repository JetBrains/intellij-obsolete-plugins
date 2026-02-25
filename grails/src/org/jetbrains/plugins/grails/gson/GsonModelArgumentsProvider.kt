/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowBuilderUtil

class GsonModelArgumentsProvider : GroovyNamedArgumentProvider() {

  override fun getNamedArguments(literal: GrListOrMap): Map<String, NamedArgumentDescriptor> {
    if (!ControlFlowBuilderUtil.isCertainlyReturnStatement(literal)) return emptyMap()

    val modelFields = getModelFields(literal)
    if (modelFields.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, NamedArgumentDescriptor>()

    for (field in modelFields) {
      result[field.name] = GsonModelFieldArgumentDescriptor(field)
    }
    return result
  }
}