package com.intellij.bigdatatools.databricks.client

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.coreUi.settings.components.RenderableEntity

enum class DatabricksConnType(override val id: String, override val title: String) : RenderableEntity {
  PROFILE("profile", DatabricksBundle.message("auth.type.profile")),
  DATABRICKS("databricks-cli", DatabricksBundle.message("auth.type.databricks.cli")),
  AZURE("azure-client-secret", DatabricksBundle.message("auth.type.azure")),
}

