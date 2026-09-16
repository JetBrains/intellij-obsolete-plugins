package com.jetbrains.bigdatatools.glue.monitoring.models

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.glue.utils.GlueLocalizedField
import software.amazon.awssdk.services.glue.model.Database

data class GlueDatabaseInfo(@NoRendering val database: Database) : RemoteInfo {
  val name: String = database.name() ?: ""
  val description: String = database.description() ?: ""
  val catalog: String = database.catalogId() ?: ""

  companion object {
    val renderableColumns: List<GlueLocalizedField<GlueDatabaseInfo>> by lazy {
      listOf(
        GlueLocalizedField(GlueDatabaseInfo::name, "data.GlueDatabaseInfo.name"),
        GlueLocalizedField(GlueDatabaseInfo::description, "data.GlueDatabaseInfo.description"),
        GlueLocalizedField(GlueDatabaseInfo::catalog, "data.GlueDatabaseInfo.catalog")
      )
    }
  }
}