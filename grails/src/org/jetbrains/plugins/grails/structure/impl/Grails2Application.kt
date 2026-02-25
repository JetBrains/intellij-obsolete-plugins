/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.config.GrailsModuleStructureUtil
import org.jetbrains.plugins.grails.config.GrailsSettingsService.getGrailsSettings
import org.jetbrains.plugins.grails.config.PrintGrailsSettingsConstants
import org.jetbrains.plugins.grails.util.version.Version
import org.jetbrains.plugins.grails.util.version.VersionImpl

internal class Grails2Application(root: VirtualFile, module: Module) : OldGrailsModuleBasedApplication(module, root) {

  override val name: String get() = getApplicationPropertiesValue("app.name") ?: module.name

  override val appVersion: String? get() = getApplicationPropertiesValue("app.version")

  val applicationPropertiesVersion: Version? get() {
    val version = getApplicationPropertiesValue(GrailsModuleStructureUtil.GRAILS_VERSION_KEY)
    return if (version.isNullOrBlank()) null else VersionImpl(version)
  }

  val isRunForked: Boolean get() = getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_RUN_FORK)

  val isTestForked: Boolean get() = getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_TEST_FORK)

  private fun getApplicationPropertiesValue(key: String) = runReadAction { applicationProperties?.getPropertyValue(key) }

  private fun getBooleanSetting(key: String) = runReadAction { getGrailsSettings(module).properties[key]?.toBoolean() ?: false }
}
