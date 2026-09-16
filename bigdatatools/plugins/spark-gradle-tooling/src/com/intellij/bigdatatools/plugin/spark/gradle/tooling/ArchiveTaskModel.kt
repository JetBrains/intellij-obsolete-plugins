package com.intellij.bigdatatools.plugin.spark.gradle.tooling

import org.gradle.tooling.model.Model
import java.io.Serializable

interface ArchiveTaskModel : Model, Serializable {

  val archiveTaskToArtifact: Map<String, String?>
}