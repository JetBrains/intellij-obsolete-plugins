/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.resolve.ExpectedPackageNameProvider

internal class GsonExpectedPackageProvider : ExpectedPackageNameProvider {
  override fun inferPackageName(file: GroovyFile): String? {
    return if (file.virtualFile.nameSequence.endsWith(GsonConstants.FILE_SUFFIX)) "" else null
  }
}