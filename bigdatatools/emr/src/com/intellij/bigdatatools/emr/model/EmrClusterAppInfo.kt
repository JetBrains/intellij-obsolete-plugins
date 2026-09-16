package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.NoRendering
import com.intellij.bigdatatools.emr.util.EmrLocalizedColumn
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.depend.BdtAppInfo

data class EmrClusterAppInfo(override val name: String,
                             val version: String = "",
                             override val url: String = "",
                             @NoRendering
                             override val connType: BdtConnectionType?,
                             val additionalParams: Map<String, Any> = emptyMap()) : BdtAppInfo {
  companion object {
    val renderableColumns: List<EmrLocalizedColumn<EmrClusterAppInfo>> by lazy {
      listOf(
        EmrLocalizedColumn(EmrClusterAppInfo::name, "data.emr.cluster.app.name"),
        EmrLocalizedColumn(EmrClusterAppInfo::version, "data.emr.cluster.app.version"),
        EmrLocalizedColumn(EmrClusterAppInfo::url, "data.emr.cluster.app.url"),
        EmrLocalizedColumn(EmrClusterAppInfo::additionalParams, "data.emr.cluster.app.additionalParams"),
      )
    }

    fun createSftp(clusterDetails: EmrClusterDetails): EmrClusterAppInfo {
      val url = clusterDetails.cluster.masterPublicDnsName()
      return EmrClusterAppInfo(name = EmrMessagesBundle.message("connection.sftp.to.master.node"), url = url,
                               connType = BdtConnectionType.SFTP)
    }
  }
}