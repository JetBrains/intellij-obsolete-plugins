package com.intellij.bigdatatools.main

import org.jetbrains.intellij.build.buildPlugin

internal object BuildBigDataToolsBuildTarget {
  @JvmStatic
  fun main(args: Array<String>) = buildPlugin(
    "intellij.bigdatatools.gcloud",
    "intellij.bigdatatools.coreUi",
    "intellij.bigdatatools.awsBase",
    "intellij.bigdatatools.common",
    "intellij.bigdatatools.core",
    "intellij.bigdatatools.binaryFilesSupport",
    "intellij.bigdatatools.plugin.rfs",
    "intellij.bigdatatools.metastore.core",
    "intellij.bigdatatools.plugin.spark",
    "intellij.bigdatatools.flink",
    "intellij.bigdatatools.databricks",
  ) {
    skipProprietaryBuildTools()
  }
}