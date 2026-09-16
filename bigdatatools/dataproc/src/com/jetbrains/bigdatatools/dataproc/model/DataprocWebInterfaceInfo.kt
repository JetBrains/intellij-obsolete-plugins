package com.jetbrains.bigdatatools.dataproc.model

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtAppInfo
import com.jetbrains.bigdatatools.dataproc.util.DataprocLocalizedField

data class DataprocWebInterfaceInfo(override val name: String,
                                    override val url: String,
                                    val componentGateway: Boolean,
                                    @NoRendering
                                    val instanceName: String = "") : BdtAppInfo {
  @NoRendering
  override val connType: BdtConnectionType? = when {
    name == "YARN ResourceManager" -> BdtConnectionType.YARN
    name == "Spark History Server" -> BdtConnectionType.SPARK_MONITORING
    name == "Zeppelin" -> BdtConnectionType.ZEPPELIN
    name == STAGE_BUCKET_NAME -> BdtConnectionType.GCS
    name == GCS_NAME -> BdtConnectionType.GCS
    instanceName != "" -> BdtConnectionType.SFTP
    else -> null
  }

  companion object {
    val GCS_NAME = "Google Cloud Storage"
    val STAGE_BUCKET_NAME = "Stage Bucket"
    val renderableColumns: List<DataprocLocalizedField<DataprocWebInterfaceInfo>> by lazy {
      listOf(
        DataprocLocalizedField(DataprocWebInterfaceInfo::name, "data.vm.instanceInfo.name"),
        DataprocLocalizedField(DataprocWebInterfaceInfo::url, "data.vm.instanceInfo.url"),
        DataprocLocalizedField(DataprocWebInterfaceInfo::componentGateway, "data.vm.instanceInfo.componentGateway"),
      )
    }
  }
}

