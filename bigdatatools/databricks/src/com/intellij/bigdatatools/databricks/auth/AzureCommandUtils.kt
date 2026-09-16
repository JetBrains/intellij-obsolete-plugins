package com.intellij.bigdatatools.databricks.auth

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


object AzureCommandUtils {
  private const val WINDOWS_STARTER = "cmd.exe"
  private const val LINUX_MAC_STARTER = "/bin/sh"
  private const val WINDOWS_SWITCHER = "/c"
  private const val LINUX_MAC_SWITCHER = "-c"
  private val DEFAULT_WINDOWS_SYSTEM_ROOT: String? = System.getenv("SystemRoot")
  private const val DEFAULT_MAC_LINUX_PATH = "/bin/"

  private val safeWorkingDirectory: String?
    get() {
      if (SystemInfo.isWindows) {
        if (DEFAULT_WINDOWS_SYSTEM_ROOT.isNullOrBlank()) {
          return null
        }
        return "$DEFAULT_WINDOWS_SYSTEM_ROOT\\system32"
      }
      else {
        return DEFAULT_MAC_LINUX_PATH
      }
    }


  fun execOrNull(commandWithArgs: String): String? {
    return try {
      val pair = exec(commandWithArgs, env = EnvironmentUtil.getEnvironmentMap())
      if (pair.second == 0) {
        pair.first
      }
      else
        null
    }
    catch (t: Throwable) {
      thisLogger().info(t)
      null
    }
  }

  @Throws(IOException::class)
  fun exec(commandWithArgs: String,
           env: Map<String, String> = HashMap(),
           cwd: String? = null,
           mergeErrorStream: Boolean = true): Pair<String, Int> {
    val starter = if (SystemInfo.isWindows) WINDOWS_STARTER else LINUX_MAC_STARTER
    val switcher = if (SystemInfo.isWindows) WINDOWS_SWITCHER else LINUX_MAC_SWITCHER
    val workingDirectory = cwd ?: safeWorkingDirectory
    if (workingDirectory.isNullOrBlank()) {
      val exception = IllegalStateException("A Safe Working directory could not be found to execute command from.")
      Logger.getInstance(AzureCommandUtils::class.java.name).error("exec", exception)
      throw exception
    }
    val commandWithPath = if (SystemInfo.isWindows) commandWithArgs else String.format("export PATH=\$PATH:/usr/local/bin ; %s", commandWithArgs)
    return executeCommandAndGetOutput(starter, switcher, commandWithPath, File(workingDirectory), env, mergeErrorStream)
  }

  @Throws(IOException::class)
  private fun executeCommandAndGetOutput(starter: String, switcher: String, commandWithArgs: String,
                                         directory: File, env: Map<String, String>, mergeErrorStream: Boolean): Pair<String, Int> {
    val commandLine = GeneralCommandLine(starter)
    commandLine.addParameter(switcher)
    commandLine.addParameter(commandWithArgs)

    return executeCommandAndGetOutput(starter, listOf(switcher, commandWithArgs), directory, env, mergeErrorStream)
  }

  @Throws(IOException::class)
  private fun executeCommandAndGetOutput(command: String,
                                         commandArgs: List<String>,
                                         directory: File,
                                         env: Map<String, String>?,
                                         mergeErrorStream: Boolean): Pair<String, Int> {
    val processBuilder = ProcessBuilder()
    processBuilder.redirectErrorStream(mergeErrorStream)
    processBuilder.directory(directory)
    processBuilder.command(command, *commandArgs.toTypedArray())
    processBuilder.environment().putAll(env ?: HashMap())

    val process = processBuilder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val text = reader.use {
      process.waitFor()
      val lines = reader.readLines()

      // For Azure we have a strange string with a string part containing a row of '32' characters,
      // and here we will replace them by single " ".
      lines.joinToString(separator = "\n") { it.replace("\\s+".toRegex(), " ") }
    }

    return text to process.exitValue()
  }
}