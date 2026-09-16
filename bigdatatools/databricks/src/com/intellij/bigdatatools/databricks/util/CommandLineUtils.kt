package com.intellij.bigdatatools.databricks.util

import com.intellij.util.EnvironmentUtil
import java.io.BufferedReader
import java.io.InputStreamReader

object CommandLineUtils {
  fun getCommandResultFirstLine(commandPath: String, vararg command: String): String {
    val processBuilder = ProcessBuilder()
    processBuilder.environment().putAll(EnvironmentUtil.getEnvironmentMap())
    processBuilder.redirectErrorStream(true)
    processBuilder.command(commandPath, *command)
    val process = processBuilder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val line = reader.readLine() ?: ""
    reader.close()
    /*val exitCode = */ process.waitFor()
    // For Azure we have a strange string with a string part containing a row of '32' characters,
    // and here we will replace them by single " ".
    return line.replace("\\s+".toRegex(), " ")
  }
}