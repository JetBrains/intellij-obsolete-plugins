/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.tests.runner

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.util.version.v13

private val testFolderNames = listOf("integration", "unit", "functional")

internal fun getParamKeyByTestRoot(application: GrailsApplication, testRoot: VirtualFile): String? {
  val name = testRoot.name
  if (name in testFolderNames) {
    return if (application.grailsVersion >= v13) {
      "$name:"
    }
    else {
      "-$name"
    }
  }
  return null
}
