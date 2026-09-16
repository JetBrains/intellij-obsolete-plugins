package com.intellij.bigdatatools.databricks.client

import com.databricks.sdk.core.CliTokenSource
import com.databricks.sdk.core.CredentialsProvider
import com.databricks.sdk.core.DatabricksCliCredentialsProvider
import com.databricks.sdk.core.DatabricksConfig
import com.databricks.sdk.core.DatabricksException
import com.databricks.sdk.core.HeaderFactory
import com.databricks.sdk.core.oauth.Token
import com.databricks.sdk.core.utils.OSUtils
import com.intellij.bigdatatools.coreUi.rfs.exception.RfsAuthRequiredError
import com.intellij.bigdatatools.databricks.cli.DatabricksCliWrapper
import kotlin.io.path.Path

class IntellijDatabricksAuthProvider(val calledByUser: Boolean) : CredentialsProvider {
  override fun authType(): String = DatabricksCliCredentialsProvider.DATABRICKS_CLI

  override fun configure(config: DatabricksConfig): HeaderFactory? {
    val host = config.host
    if (host == null) {
      return null
    }

    val token = getOrLogin(config)
    return HeaderFactory {
      val headers: MutableMap<String, String> = HashMap()
      headers["Authorization"] = token.tokenType + " " + token.accessToken
      headers
    }
  }

  private fun getOrLogin(config: DatabricksConfig): Token {
    val tokenSource = getDatabricksCliTokenSource(config)
    return try {
      tokenSource.token
    }
    catch (t: DatabricksException) {
      if (calledByUser) {
        login(config)
      }
      else {
        throw RfsAuthRequiredError()
      }
      tokenSource.token
    }
  }

  private fun login(config: DatabricksConfig) {
    val cliPath = getCliPath(config)
    val cliWrapper = DatabricksCliWrapper(Path(cliPath), null)
    cliWrapper.runDatabricksLogin(cliPath, config.host)

  }

  private fun getDatabricksCliTokenSource(config: DatabricksConfig): CliTokenSource {
    val cliPath = getCliPath(config)

    val cmd = mutableListOf(cliPath, "auth", "token", "--host", config.host)
    if (config.isAccountClient) {
      cmd.add("--account-id")
      cmd.add(config.accountId)
    }
    return CliTokenSource(cmd, "token_type", "access_token", "expiry", config.env)
  }

  private fun getCliPath(config: DatabricksConfig): String {
    var cliPath = config.databricksCliPath
    if (cliPath == null) {
      cliPath = OSUtils.get(config.env).databricksCliPath
    }
    if (cliPath == null) {
      throw DatabricksException("Databricks CLI could not be found")
    }
    return cliPath
  }
}