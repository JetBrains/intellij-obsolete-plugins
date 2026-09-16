package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.emr.client.BdtEmrClient
import com.intellij.openapi.util.use
import software.amazon.awssdk.services.emr.model.ClusterSummary

object EmrConnectionChecker {
  fun checkConnection(connectionData: EmrConnectionData): List<ClusterSummary> {
    val client = BdtEmrClient(null, connectionData)

    return client.use {
      it.connect(true)
      it.getClusters(limit = 1)
    }
  }
}