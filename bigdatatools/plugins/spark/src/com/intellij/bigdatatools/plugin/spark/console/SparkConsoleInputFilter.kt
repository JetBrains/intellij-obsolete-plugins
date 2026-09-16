package com.intellij.bigdatatools.plugin.spark.console

import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Pair

class SparkConsoleInputFilter : InputFilter {
  private var prevLevel: LogLevel = LogLevel.INFO

  override fun applyFilter(text: String, contentType: ConsoleViewContentType): List<Pair<String, ConsoleViewContentType>>? {
    if (contentType != ConsoleViewContentType.ERROR_OUTPUT)
      return null
    else {
      listOf(Pair.create(text, ConsoleViewContentType.NORMAL_OUTPUT))
    }

    val level = LogLevel.entries.firstOrNull { text.contains(it.name) } ?: prevLevel
    prevLevel = level
    return listOf(Pair.create(text, prevLevel.type))
  }

  enum class LogLevel(val type: ConsoleViewContentType) {
    FATAL(ConsoleViewContentType.LOG_ERROR_OUTPUT),
    ERROR(ConsoleViewContentType.LOG_ERROR_OUTPUT),
    WARN(ConsoleViewContentType.LOG_ERROR_OUTPUT),
    INFO(ConsoleViewContentType.NORMAL_OUTPUT),
    DEBUG(ConsoleViewContentType.LOG_DEBUG_OUTPUT),
    TRACE(ConsoleViewContentType.LOG_VERBOSE_OUTPUT),
  }
}