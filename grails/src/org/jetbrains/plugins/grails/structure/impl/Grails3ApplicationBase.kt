/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.gradle.GrailsModuleData
import org.jetbrains.plugins.grails.structure.Grails3Application
import org.jetbrains.plugins.grails.structure.GrailsApplicationBase
import org.jetbrains.plugins.grails.util.version.VersionImpl

internal abstract class Grails3ApplicationBase(
    project: Project,
    root: VirtualFile,
    private val moduleDataNode: DataNode<ModuleData>
) : GrailsApplicationBase(project, root), Grails3Application {

  override val name: String get() = moduleDataNode.data.externalName

  override val appVersion: String? get() = moduleDataNode.data.version

  override val grailsVersion by lazyUnsafe { VersionImpl(gradleData.grailsVersion) }

  override val gradleData by lazyUnsafe { ExternalSystemApiUtil.find(moduleDataNode, GrailsModuleData.KEY)!!.data }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as Grails3ApplicationBase

    return moduleDataNode == other.moduleDataNode
  }

  override fun hashCode() = 31 * super.hashCode() + moduleDataNode.hashCode()
}