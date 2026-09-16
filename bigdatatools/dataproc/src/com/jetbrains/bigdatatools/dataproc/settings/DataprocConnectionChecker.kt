package com.jetbrains.bigdatatools.dataproc.settings

import com.google.cloud.dataproc.v1.Cluster
import com.intellij.openapi.util.use
import com.jetbrains.bigdatatools.dataproc.client.BdtDataprocClient

object DataprocConnectionChecker {
  fun checkConnection(connectionData: DataprocConnectionData): List<Cluster> {
    val client = BdtDataprocClient(null, connectionData)

    return client.use {
      it.connect(true)
      it.getClusters(limit = 1, supportedStates = emptyList())
    }
  }
}