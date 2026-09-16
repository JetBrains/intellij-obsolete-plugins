package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * DAO object to display Application timeout information.
 */
data class AppTimeoutInfo(val timeoutType: ApplicationTimeoutType? = null,
                          val expiryTime: String = "UNLIMITED",
                          var remainingTimeInSec: Long = -1)