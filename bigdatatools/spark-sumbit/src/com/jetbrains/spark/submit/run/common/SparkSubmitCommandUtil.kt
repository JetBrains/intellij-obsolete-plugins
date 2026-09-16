package com.jetbrains.spark.submit.run.common

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil

object SparkSubmitCommandUtil {

  fun createCommandLine(
    shellExecutor: List<String>?,
    interactiveModeKey: String?,
    beforeScript: String?,
    commandSeparator: String,
    command: List<String>,
  ): GeneralCommandLine {
    return if (shellExecutor != null || beforeScript != null || interactiveModeKey != null) {
      requireNotNull(shellExecutor)
      val beforeCommand = beforeScript?.let { "${beforeScript} $commandSeparator " }
      GeneralCommandLine(shellExecutor + listOfNotNull(interactiveModeKey,
                                                       (beforeCommand ?: "") + ParametersListUtil.join(command, ::escapeArgument)))
    }
    else {
      GeneralCommandLine(command)
    }
  }

  private val noEscapeChars = Regex("[^a-zA-Z0-9,._+=:@%/-]")

  /**
   * Tweaked version of [ParametersListUtil.escape], a bit more suitable for passing as a line to shell interpreter
   */
  private fun escapeArgument(argument: CharSequence): String {
    val builder = StringBuilder(argument)
    StringUtil.escapeQuotes(builder)
    if (builder.isEmpty() || builder.contains(noEscapeChars)) {
      // don't let a trailing backslash (if any) unintentionally escape the closing quote
      val numTrailingBackslashes = builder.length - StringUtil.trimTrailing(builder, '\\').length
      StringUtil.quote(builder)
      StringUtil.repeatSymbol(builder, '\\', numTrailingBackslashes)
    }
    return builder.toString()
  }

}