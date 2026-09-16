package com.intellij.bigdatatools.zeppelin.dependency

import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk

interface NoteDependencyManager : Disposable {
  fun getSdk(project: Project): Sdk?

  fun isResolving(project: Project): Boolean

  fun getAllLibraries(): List<ZeppelinLibrary>

  fun isLibraryResolved(project: Project, library: ZeppelinLibrary): Boolean

  fun launchResolveDependenciesForAllProject(force: Boolean = false)
  fun updateUserLibs(libs: List<ZeppelinLibrary>) {}
}