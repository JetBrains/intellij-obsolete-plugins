package com.intellij.bigdatatools.zeppelin.dependency.model

import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency

data class ZeppelinLibrary(val dep: ZepDependency, val type: ZeppelinLibraryType, val from: String = "") {
  val presentableName: String
    get() = type.prefix + if (dep.isModule())
      dep.moduleName.trim() + " module"
    else
      dep.groupArtifactVersion.trim()
}