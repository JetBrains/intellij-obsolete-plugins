/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure

import org.jetbrains.plugins.grails.gradle.GrailsModuleData

interface Grails3Application : GrailsApplication {

  val gradleData: GrailsModuleData
}
