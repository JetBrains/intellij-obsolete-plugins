package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import org.jetbrains.annotations.Nls

enum class DatabricksRunMode(@Nls val label: String) {
  WORKFLOW(DatabricksBundle.message("run.mode.workflow")),
  SERVER(DatabricksBundle.message("run.mode.server"))
}