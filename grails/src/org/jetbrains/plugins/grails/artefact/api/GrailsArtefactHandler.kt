/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.api

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.structure.GrailsApplication

interface GrailsArtefactHandler {

  val artefactHandlerID: String

  val artefactClassSuffix: String get() = artefactHandlerID

  fun getDirectory(application: GrailsApplication): VirtualFile? = null

  val annotationFqns: Collection<String> get() = emptyList()

}