/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.grails.util.version.Version
import javax.swing.Icon

interface GrailsApplication : UserDataHolderEx {

  val name: @NlsSafe String

  val appVersion: String? get() = null

  val icon: Icon

  val project: Project

  /**
   * @return folder that contains grails-app, application.properties, etc.
   */
  val root: VirtualFile

  /**
   * @return `grails-app` folder
   */
  val appRoot: VirtualFile

  val grailsVersion: Version

  val isValid: Boolean

  fun invalidate()

  fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope
}
