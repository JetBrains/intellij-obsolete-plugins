package com.jetbrains.spark.submit.model

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import org.jetbrains.annotations.NonNls

enum class DeployModeType(@NlsContexts.Label val value: String, @NonNls val valueForCommand: String) {
  CLIENT(SparkMessagesBundle.message("settings.deploymode.client"), "client"),
  CLUSTER(SparkMessagesBundle.message("settings.deploymode.cluster"), "cluster");
}