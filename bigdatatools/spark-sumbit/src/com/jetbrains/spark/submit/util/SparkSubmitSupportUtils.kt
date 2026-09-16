package com.jetbrains.spark.submit.util

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.packaging.artifacts.ArtifactManager

object SparkSubmitSupportUtils {
  fun isArtifactSupported() =
    try {
      ArtifactManager.TOPIC
      true
    }
    catch (e: Throwable) {
      false
    }

  fun isWebDeploymentSupported(): Boolean {
    val plugin = PluginManager.getInstance().findEnabledPlugin(PluginId.getId("com.jetbrains.plugins.webDeployment"))
    return plugin?.isEnabled == true
  }
}