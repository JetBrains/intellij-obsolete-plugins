package com.intellij.bigdatatools.plugin.spark.gradle.tooling

class DefaultArchiveTaskModel(
  override val archiveTaskToArtifact: Map<String, String?>
) : ArchiveTaskModel