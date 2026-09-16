package com.jetbrains.spark.submit.model

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.spark.submit.util.SparkMessagesBundle

enum class ClusterManagerType(@NlsContexts.Label val value: String, val masterFixed: String?, val masterOptions: List<String>) {
  LOCAL(SparkMessagesBundle.message("cluster.manager.local"), null, listOf("local", "local[*]", "local[K]", "local[K,F]")),
  STANDALONE(SparkMessagesBundle.message("cluster.manager.standalone"), null, listOf("spark://")),
  MESOS(SparkMessagesBundle.message("cluster.manager.mesos"), null, listOf("mesos://")),
  YARN(SparkMessagesBundle.message("cluster.manager.yarn"), "yarn", emptyList()),
  KUBERNETES(SparkMessagesBundle.message("cluster.manager.kubernetes"), null, listOf("k8s://")),
  NOMAD(SparkMessagesBundle.message("cluster.manager.nomad"), "nomad", emptyList());

  companion object {
    fun fromMaster(master: String): ClusterManagerType {
      for (clusterManagerType in ClusterManagerType.entries) {
        if (clusterManagerType.masterFixed == master) return clusterManagerType
        if (master.startsWith(clusterManagerType.masterOptions.first())) return clusterManagerType
      }
      return STANDALONE
    }
  }
}