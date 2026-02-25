/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import org.jetbrains.plugins.grails.tests.GrailsTestUtils
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.util.GroovyUtils

val GRAILS_APPLICATION: DataKey<GrailsApplication> = DataKey.create("grails.application")
val GRAILS_ARTEFACT_HANDLER: DataKey<GrailsArtefactHandler> = DataKey.create("grails.artefact.handler")
val GRAILS_ARTEFACT_PACKAGE: DataKey<String> = DataKey.create("grails.artefact.package")

class ArtefactData(
    val project: Project,
    val module: Module,
    val file: VirtualFile,
    val packageName: String?,
    val artefactName: String,
    val application: GrailsApplication,
    val isView: Boolean = false
)

fun getArtefactData(context: DataContext?): ArtefactData? {
  if (context == null) return null
  val project = context.getData(LangDataKeys.PROJECT) ?: return null
  if (DumbService.isDumb(project)) return null
  val module = context.getData(PlatformCoreDataKeys.MODULE) ?: return null
  val file = context.getData(LangDataKeys.VIRTUAL_FILE) ?: return null
  val application = GrailsApplicationManager.getInstance(project).findApplication(file) ?: return null
  val publicClass = GroovyUtils.getPublicClass(project, file)

  val isView: Boolean
  val packageName: String?
  val artefactName: String

  if (publicClass == null) {
    // inside a view
    val controllerName = GrailsUtils.getControllerNameByGsp(file) ?: return null
    if (controllerName == "layouts" || !StringUtil.isJavaIdentifier(controllerName)) return null
    isView = true
    packageName = null // we do not know package here
    artefactName = controllerName
  }
  else {
    val artefactClass = if (GrailsUtils.isInGrailsTests(file, project)) {
      GrailsTestUtils.getTestedClass(publicClass)
    }
    else {
      publicClass
    }
    artefactClass ?: return null
    isView = false
    packageName = StringUtil.getPackageName(artefactClass.qualifiedName ?: return null)
    artefactName = GrailsArtifact.getType(artefactClass)?.getArtifactName(artefactClass) ?: return null
  }

  return ArtefactData(project, module, file, packageName, artefactName, application, isView)
}

fun getGrailsApplication(dataContext: DataContext): GrailsApplication? {
  dataContext.getData(GRAILS_APPLICATION)?.let { return it }
  val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return null
  val instance = GrailsApplicationManager.getInstance(project)
  val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return null
  return instance.findApplication(virtualFile)
}

fun getArtefactHandler(dataContext: DataContext): GrailsArtefactHandler? = dataContext.getData(GRAILS_ARTEFACT_HANDLER)

fun getArtefactPackage(dataContext: DataContext): String? = dataContext.getData(GRAILS_ARTEFACT_PACKAGE)