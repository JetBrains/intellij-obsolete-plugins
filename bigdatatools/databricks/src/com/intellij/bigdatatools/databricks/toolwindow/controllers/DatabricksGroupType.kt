package com.intellij.bigdatatools.databricks.toolwindow.controllers

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import org.jetbrains.annotations.Nls

enum class DatabricksGroupType(@Nls val title: String) {
  CONF(DatabricksBundle.message("controller.conf")),
  WORKFLOW_LIST(DatabricksBundle.message("controller.workflow")),
  SERVER_RUNS_LIST(DatabricksBundle.message("controller.server.runs")),
  WORKFLOW_RUN_DETAILS(DatabricksBundle.message("controller.workflow.run-details")),
  SERVER_RUN_DETAILS(DatabricksBundle.message("controller.server.run-details")),
}