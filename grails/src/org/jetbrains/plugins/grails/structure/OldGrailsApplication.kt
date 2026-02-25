/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module

interface OldGrailsApplication : GrailsApplication {

  val module: Module

  val applicationProperties: PropertiesFile?
}
