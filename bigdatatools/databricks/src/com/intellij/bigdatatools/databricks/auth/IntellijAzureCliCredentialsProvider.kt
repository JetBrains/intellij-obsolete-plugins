package com.intellij.bigdatatools.databricks.auth

import com.databricks.sdk.core.AzureCliCredentialsProvider
import com.databricks.sdk.core.CliTokenSource
import com.databricks.sdk.core.CredentialsProvider
import com.databricks.sdk.core.DatabricksConfig
import com.databricks.sdk.core.DatabricksException
import com.databricks.sdk.core.HeaderFactory
import com.databricks.sdk.core.utils.AzureUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.thisLogger

class IntellijAzureCliCredentialsProvider : CredentialsProvider {
  private val mapper = ObjectMapper()
  override fun authType() = AzureCliCredentialsProvider.AZURE_CLI

  private fun tokenSourceFor(config: DatabricksConfig, resource: String): CliTokenSource {
    val cmd = mutableListOf(AzureCliWrapper.getAzPath(), "account", "get-access-token", "--resource", resource, "--output", "json")

    val subscription = getSubscription(config)
    if (subscription != null) {
      // This will fail if the user has access to the workspace, but not to the subscription
      // itself.
      // In such case, we fall back to not using the subscription.
      cmd += listOf("--subscription", subscription)
      try {
        return getToken(config, cmd)
      }
      catch (ex: DatabricksException) {
        throw DatabricksException("Failed to get token for subscription. Using resource only token.", ex)
      }
    }
    else {
      thisLogger().warn(
        "azure_workspace_resource_id field not provided. "
        + "It is recommended to specify this field in the Databricks configuration to avoid authentication errors.")
    }

    return getToken(config, cmd)
  }

  protected fun getToken(config: DatabricksConfig, cmd: List<String>?): CliTokenSource {
    val token = CliTokenSource(cmd, "tokenType", "accessToken", "expiresOn", config.env)
    token.token // We need this to check if the CLI is installed and to validate the config.
    return token
  }

  private fun getSubscription(config: DatabricksConfig): String? {
    val resourceId = config.azureWorkspaceResourceId
    if (resourceId == null || resourceId == "") {
      return null
    }
    val components = resourceId.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (components.size < 3) {
      thisLogger().warn("Invalid azure workspace resource ID")
      return null
    }
    return components[2]
  }

  override fun configure(config: DatabricksConfig): HeaderFactory {
    if (!config.isAzure) {
      throw DatabricksException("This connection is not Azure connection")
    }

    AzureUtils.ensureHostPresent(config, mapper) { it: DatabricksConfig, resource: String -> this.tokenSourceFor(it, resource) }
    val resource = config.effectiveAzureLoginAppId
    val tokenSource = tokenSourceFor(config, resource)
    var mgmtTokenSource: CliTokenSource?
    try {
      mgmtTokenSource =
        tokenSourceFor(config, config.azureEnvironment.serviceManagementEndpoint)
    }
    catch (e: Exception) {
      thisLogger().debug("Not including service management token in headers", e)
      mgmtTokenSource = null
    }
    val finalMgmtTokenSource = mgmtTokenSource

    return HeaderFactory {
      val token = tokenSource.token
      val headers: MutableMap<String, String> = HashMap()
      headers["Authorization"] = token.tokenType + " " + token.accessToken
      if (finalMgmtTokenSource != null) {
        AzureUtils.addSpManagementToken(finalMgmtTokenSource, headers)
      }
      AzureUtils.addWorkspaceResourceId(config, headers)
    }
  }
}
