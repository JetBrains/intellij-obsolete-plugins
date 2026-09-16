package com.jetbrains.spark.monitoring.data

import com.intellij.openapi.util.NlsSafe

data class AppAttemptId(@NlsSafe val appId: String, val attemptId: String?) {
  private val isFirstTry = attemptId == null || attemptId == "" || attemptId == "1"

  override fun toString() = attemptId?.ifBlank { null }?.let { "${appId}/$attemptId" } ?: appId
  fun toUnderscoreString() = attemptId?.takeIf { it.isNotBlank() && it != "1" }?.let { "${appId}_$attemptId" } ?: appId
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppAttemptId

    if (appId != other.appId) return false
    if (isFirstTry && other.isFirstTry) return true
    if (attemptId != other.attemptId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = appId.hashCode()
    result = 31 * result + (attemptId?.hashCode() ?: 0)
    return result
  }


}
