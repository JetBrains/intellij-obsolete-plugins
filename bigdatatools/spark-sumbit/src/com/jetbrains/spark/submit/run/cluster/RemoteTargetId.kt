package com.jetbrains.spark.submit.run.cluster

class RemoteTargetId(var connectionId: String? = null, var clusterId: String? = null, var name: String? = null) {
  override fun equals(other: Any?): Boolean {
    return other is RemoteTargetId && connectionId == other.connectionId && clusterId == other.clusterId
  }
  override fun hashCode(): Int {
    return connectionId.hashCode() + 3 * clusterId.hashCode()
  }
}