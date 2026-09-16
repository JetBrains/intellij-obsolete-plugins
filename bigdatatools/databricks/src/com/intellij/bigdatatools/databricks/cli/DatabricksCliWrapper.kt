package com.intellij.bigdatatools.databricks.cli

import com.databricks.sdk.core.DatabricksConfig
import com.intellij.bigdatatools.databricks.client.DatabricksProxySupport
import com.intellij.bigdatatools.databricks.util.CommandLineUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.EnvironmentUtil
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class DatabricksCliWrapper(private val cliPath: Path, val project: Project?) {
  fun getSyncCommand(remoteFile: RfsPath, config: DatabricksConfig?): GeneralCommandLine {
    val projectDir = project?.guessProjectDir()
    val envs = mapEnvs(config!!)
    val commandLine = GeneralCommandLine(cliPath.absolutePathString(), "sync", ".", remoteFile.stringRepresentation(),
                                         "--watch", "--output", "json").withEnvironment(envs)
    commandLine.withWorkingDirectory(Path.of(projectDir?.canonicalPath ?: "."))
    return commandLine
  }

  fun runDatabricksLogin(cliPath: String, host: String): String {
    val result = CommandLineUtils.getCommandResultFirstLine(cliPath, "auth", "login", "--host", host)
    return result
  }


  private fun mapEnvs(config: DatabricksConfig): Map<String, String> {
    val environment = EnvironmentUtil.getEnvironmentMap()
    val transferred = mapOf(
      "DATABRICKS_HOST" to config.host,
      "DATABRICKS_ACCOUNT_ID" to config.accountId,
      "DATABRICKS_CLIENT_ID" to config.clientId,
      "DATABRICKS_CLIENT_SECRET" to config.clientSecret,
      "DATABRICKS_SCOPES" to config.scopes?.joinToString(","),
      "DATABRICKS_REDIRECT_URL" to config.oAuthRedirectUrl,
      "DATABRICKS_USERNAME" to config.username,
      "DATABRICKS_PASSWORD" to config.password,
      "DATABRICKS_CONFIG_PROFILE" to config.profile,
      "DATABRICKS_CONFIG_FILE" to config.configFile,
      "DATABRICKS_CLUSTER_ID" to config.clusterId,
      "DATABRICKS_GOOGLE_SERVICE_ACCOUNT" to config.googleServiceAccount,
      "GOOGLE_CREDENTIALS" to config.googleCredentials,
      "DATABRICKS_AZURE_RESOURCE_ID" to config.azureWorkspaceResourceId,
      "ARM_CLIENT_SECRET" to config.azureClientSecret,
      "ARM_CLIENT_ID" to config.clientId,
      "ARM_TENANT_ID" to config.azureTenantId,
      "DATABRICKS_CLI_PATH" to config.databricksCliPath,
      "DATABRICKS_AUTH_TYPE" to config.authType,
      "DATABRICKS_CLI_UPSTREAM" to "databricks-intellij",
      "DATABRICKS_CLI_UPSTREAM_VERSION" to BDTPluginUtil.getDatabricksVersion(),
      ).filterValues { it != null }.mapValues { it.value!! }
    return environment + transferred + DatabricksProxySupport.proxyEnvironment(config.host)
  }
}
