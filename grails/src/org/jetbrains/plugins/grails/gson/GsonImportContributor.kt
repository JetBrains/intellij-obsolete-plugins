/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.gson

import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.resolve.imports.GrImportContributor
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyImport
import org.jetbrains.plugins.groovy.lang.resolve.imports.StaticStarImport

class GsonImportContributor : GrImportContributor {

  private val imports: List<GroovyImport> by lazy {
    mutableListOf<GroovyImport>().apply {
      add(StaticStarImport("org.springframework.http.HttpStatus"))
      add(StaticStarImport("org.springframework.http.HttpMethod"))
      add(StaticStarImport("grails.web.http.HttpHeaders"))
    }
  }

  override fun getFileImports(file: GroovyFile): List<GroovyImport> = if (isGsonFile(file)) imports else emptyList()
}
