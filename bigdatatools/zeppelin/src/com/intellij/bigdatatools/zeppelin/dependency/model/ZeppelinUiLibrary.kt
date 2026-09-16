package com.intellij.bigdatatools.zeppelin.dependency.model

import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency

data class ZeppelinUiLibrary(var groupArtifactId: String,
                             var excludes: List<String>,
                             val type: ZeppelinLibraryType,
                             var status: LibraryResolveStatus,
                             val from: String = "") {
  fun getDep() = ZepDependency(groupArtifactId, excludes)
  fun getLibrary() = ZeppelinLibrary(getDep(), type)

  companion object {
    fun create(lib: ZeppelinLibrary, status: LibraryResolveStatus) =
      ZeppelinUiLibrary(lib.dep.groupArtifactVersion, lib.dep.exclusions, lib.type, status, from = lib.from)
  }
}