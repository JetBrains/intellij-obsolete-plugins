package com.jetbrains.spark.submit.settings

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.spark.submit.util.SparkMessagesBundle

enum class RunConfigurationBlockType(@NlsSafe val title: String, val visibleByDefault: Boolean = false) {
  SPARK_CONFIG(SparkMessagesBundle.message("settings.spark.title")),
  SPARK_DEBUG(SparkMessagesBundle.message("settings.debug.title")),
  DEPENDENCIES(SparkMessagesBundle.message("settings.dependencies.title")),
  MAVEN_DEPENDENCIES(SparkMessagesBundle.message("settings.maven.title")),
  DRIVER(SparkMessagesBundle.message("settings.driver.title")),
  EXECUTOR(SparkMessagesBundle.message("settings.executor.title")),
  KERBEROS(SparkMessagesBundle.message("settings.kerberos.title")),
  INTEGRATION(SparkMessagesBundle.message("settings.integration.title")),
  ADDITIONAL(SparkMessagesBundle.message("settings.additional.title")),
  SHELL_OPTIONS(SparkMessagesBundle.message("settings.shell.title")),
  SSH_OPTIONS(SparkMessagesBundle.message("settings.ssh.title"))
}