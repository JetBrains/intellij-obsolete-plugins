package com.jetbrains.hadoop.monitoring.settings

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.bigdatatools.common.ui.filter.DatePeriodType
import java.util.Date

// Config for specific connection.
data class HadoopConfig(
  var selectedPage: String = "",

  var applicationDetailsProportion: Float = 0.5f,
  var detailsAttemptsProportion: Float = 0.3f,

  var logToContentProportion: Float = 0.4f,

  var expandedTools: MutableSet<ToolCategory> = mutableSetOf(),

  var scrollLogToBottom: Boolean = false,

  /** Proportions from Single application page */
  var standaloneApplicationProportion: Float = 0.25f,
  var standaloneDiagnosticsProportion: Float = 0.25f,
  var standaloneAttemptsProportion: Float = 0.5f,

  var standaloneShowDiagnostics: Boolean = true,
  var standaloneShowAttempts: Boolean = true,
  var standaloneShowContainers: Boolean = true,

  var appShowDetails: Boolean = true,
  var appShowAttempts: Boolean = true,

  var scrollDiagnosticsToBottom: Boolean = false,

  var applicationsLimit: Int? = 100,
  @NlsSafe var applicationsUser: String? = null,

  var applicationStartedPeriodType: DatePeriodType = DatePeriodType.SPECIFIED,
  var applicationsStartedBegin: Date? = null,
  var applicationsStartedEnd: Date? = null,

  var applicationFinishedPeriodType: DatePeriodType = DatePeriodType.SPECIFIED,
  var applicationsFinishedBegin: Date? = null,
  var applicationsFinishedEnd: Date? = null
)