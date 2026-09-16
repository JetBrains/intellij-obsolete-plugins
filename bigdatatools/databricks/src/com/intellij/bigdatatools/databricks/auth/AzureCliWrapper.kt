package com.intellij.bigdatatools.databricks.auth

import com.intellij.execution.CommandLineUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import java.io.File

object AzureCliWrapper {
  fun getAzPath(): String = if (SystemInfo.isWindows) "az" else checkStandardRoots() ?: detectAzurePath() ?: "az"

  private fun fastAzPath(): String = if (SystemInfo.isWindows) "az" else checkStandardRoots() ?: "az"

  @NlsSafe
  fun getAzureCLIVersion(): String? {
    val command = CommandLineUtil.toCommandLine(listOf(fastAzPath(), "--version")).joinToString(" ")
    val res = AzureCommandUtils.execOrNull(command)
    return res?.takeWhile { it != '\n' }
  }

  @NlsSafe
  fun getAzureAccount(): String? {
    val command = CommandLineUtil.toCommandLine(listOf(fastAzPath(), "account", "show")).joinToString(" ")

    val res = AzureCommandUtils.execOrNull(command) ?: return null
    if (res.contains("Please run 'az login' to setup account."))
      return null
    try {
      val json = BdtJson.fromJsonToMapStringAny(res)
      @Suppress("UNCHECKED_CAST")
      return (json["user"] as? Map<String, Any>)?.get("name") as? String
    }
    catch (t: Throwable) {
      return null
    }
  }

  private fun detectAzurePath(): String? {
    try {
      val command = CommandLineUtil.toCommandLine(listOf(fastAzPath(), if (SystemInfo.isWindows) "where" else "which", "az")).joinToString(" ")
      val res = AzureCommandUtils.execOrNull(command) ?: return null
      return if (res.isBlank() || !File(res).exists())
        null
      else
        res
    }
    catch (t: Throwable) {
      thisLogger().warn("Cannot detect Azure CLI exec.", t)
      return null
    }
  }

  /**
   * In some cases we cannot get path to az from command line, so we try to check default folders
   */
  private fun checkStandardRoots(): String? {
    return when {
      SystemInfo.isWindows -> {
        val path = System.getenv("ProgramFiles")?.let { "$it\\Microsoft SDKs\\Azure\\CLI2\\wbin\\az.cmd" } ?: return null
        if (File(path).exists()) path
        else
          null
      }
      SystemInfo.isMac -> {
        val path = "/usr/local/bin/az"
        if (File(path).exists())
          return path
        val path2 = "/opt/homebrew/bin/az"
        if (File(path2).exists())
          return path2
        return null
      }
      SystemInfo.isLinux -> {
        val path = "/usr/bin/az"
        if (File(path).exists())
          return path
        return null
      }
      else -> return null
    }
  }
}