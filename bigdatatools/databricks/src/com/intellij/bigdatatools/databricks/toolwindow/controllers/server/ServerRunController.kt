package com.intellij.bigdatatools.databricks.toolwindow.controllers.server

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import javax.swing.JComponent

class ServerRunController : ComponentController {
  override fun getComponent(): JComponent {
    return ComponentController.createInfoPanel(DatabricksBundle.message("server.run.info"))
  }

  override fun dispose() {
  }
}