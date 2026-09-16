package com.intellij.bigdatatools.zeppelin.dependency.model

sealed class ZeppelinLibraryType(val prefix: String) {
  object INTERPRETER : ZeppelinLibraryType("(Sync) Zeppelin: ")
  object BUILTIN : ZeppelinLibraryType("(Sync) Interpreter: ")
  object USER : ZeppelinLibraryType("User: ")
}