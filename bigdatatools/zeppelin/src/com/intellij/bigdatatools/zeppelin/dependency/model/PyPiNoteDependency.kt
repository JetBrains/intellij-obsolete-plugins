package com.intellij.bigdatatools.zeppelin.dependency.model

data class PyPiNoteDependency(val coords: String, val repo: String?) : NoteDependency