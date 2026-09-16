package com.jetbrains.spark.submit.run.cluster

enum class RemoteTargetType {
  EMR, DATAPROC, ARBITRARY_CLUSTER, SSH, ADD_EMR, ADD_DATAPROC, ADD_ARBITRARY_CLUSTER
}